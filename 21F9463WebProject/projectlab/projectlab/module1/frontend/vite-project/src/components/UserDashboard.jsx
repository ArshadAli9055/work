
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import io from 'socket.io-client';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import './UserDashboard.css';

// Initialize Socket.IO with Shipment microservice URL
const socket = io(import.meta.env.VITE_API_URL_SHIPMENT || 'http://localhost:3001', {
  reconnection: true,
  reconnectionAttempts: 5,
  auth: { token: localStorage.getItem('token') },
  transports: ['websocket', 'polling'],
});

function UserDashboard() {
  const [userData, setUserData] = useState(() => {
    const role = localStorage.getItem('role');
    console.log('Initial userData role:', role);
    return { role: role || 'user' };
  });
  const [recentShipments, setRecentShipments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [filters, setFilters] = useState({
    status: '',
    priority: '',
    sortBy: 'createdAt',
    order: 'desc',
  });
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');
    const userId = localStorage.getItem('userId');

    console.log('Initial localStorage values:', { token, role, userId });

    if (!token || !userId) {
      console.warn('Missing critical authentication data, redirecting to login');
      navigate('/');
    } else if (role === 'admin') {
      console.log('Admin role detected, redirecting to admin dashboard');
      navigate('/admin-dashboard');
    } else {
      setUserData((prev) => ({ ...prev, role: role || 'user' }));
      fetchRecentShipments();
      socket.on('connect', () => {
        console.log('Socket.IO connected to Shipment service:', socket.id);
      });
      socket.on(`statusUpdate:${userId}`, (data) => {
        console.log('Status update received:', data);
        toast.info(`Shipment ${data.shipmentId} updated to ${data.status}`);
        fetchRecentShipments();
      });
      socket.on('connect_error', (err) => {
        console.error('Socket.IO connection error:', err.message);
        setError('Failed to connect to real-time updates. Please refresh the page.');
      });
      socket.on('error', (err) => {
        console.error('Socket.IO error:', err);
        setError('Real-time updates error: ' + err);
      });
    }
    return () => {
      socket.off('connect');
      socket.off(`statusUpdate:${userId}`);
      socket.off('connect_error');
      socket.off('error');
    };
  }, [navigate, filters]);

  const fetchRecentShipments = async () => {
    try {
      setLoading(true);
      setError('');
      const token = localStorage.getItem('token');
      const query = new URLSearchParams(filters).toString();
      const baseUrl = import.meta.env.VITE_API_URL_SHIPMENT || 'http://localhost:3001';
      const url = `${baseUrl}/api/user/shipments?${query}`;
      console.log('Fetching shipments from:', url);
      const response = await axios.get(url, {
        headers: { Authorization: `Bearer ${token}` },
      });
      console.log('Fetch shipments response:', response.data);
      setRecentShipments(response.data);
    } catch (err) {
      console.error('Fetch shipments error:', err);
      setError(err.response?.data?.error || 'Failed to load recent shipments');
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    if (window.confirm('Are you sure you want to log out?')) {
      localStorage.removeItem('token');
      localStorage.removeItem('role');
      localStorage.removeItem('userId');
      localStorage.removeItem('tempTrackingId');
      navigate('/');
    }
  };

  const handleAddProduct = () => {
    navigate('/add-product');
  };

  const handleTrackProduct = () => {
    navigate('/track-product');
  };

  const getStatusClass = (status) => {
    switch (status) {
      case 'Shipped': return 'status-badge status-shipped';
      case 'Delivered': return 'status-badge status-delivered';
      case 'Received': return 'status-badge status-received';
      case 'Lost': return 'status-badge status-lost';
      default: return 'status-badge status-pending';
    }
  };

  return (
    <div className="dashboard-container container mt-5">
      <ToastContainer />
      <div className="dashboard-header row mb-4">
        <div className="col">
          <h2 className="dashboard-title">
            {userData.role.charAt(0).toUpperCase() + userData.role.slice(1)} Dashboard
          </h2>
        </div>
        <div className="col-auto">
          <button onClick={logout} className="btn-dashboard-danger">Logout</button>
        </div>
      </div>

      <div className="row mb-4">
        <div className="col-md-6">
          <div className="dashboard-card card">
            <div className="card-body">
              <h5 className="card-title">Welcome back!</h5>
              <p className="card-text">You are logged in as <strong>{userData.role}</strong></p>
              <p className="card-text">Use the options below to manage your shipments.</p>
              <div className="d-flex gap-2">
                <button onClick={handleAddProduct} className="btn-dashboard-primary">Add Shipment</button>
                <button onClick={handleTrackProduct} className="btn-dashboard-primary">Track Shipment</button>
              </div>
            </div>
          </div>
        </div>

        <div className="col-md-6">
          <div className="dashboard-card card">
            <div className="card-body">
              <h5 className="card-title">Quick Actions</h5>
              <div className="quick-actions">
                <button onClick={handleAddProduct} className="quick-action-btn">
                  <i className="bi bi-plus-circle"></i>Ship New Product
                </button>
                <button onClick={handleTrackProduct} className="quick-action-btn">
                  <i className="bi bi-search"></i>Track Existing Shipment
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="row mb-4">
        <div className="col-12">
          <div className="dashboard-card card">
            <div className="card-header">
              <h5 className="mb-0">Filter Shipments</h5>
            </div>
            <div className="card-body">
              <div className="row">
                <div className="col-md-3">
                  <label>Status</label>
                  <select
                    className="form-control"
                    value={filters.status}
                    onChange={(e) => setFilters({ ...filters, status: e.target.value })}
                  >
                    <option value="">All</option>
                    <option value="Pending">Pending</option>
                    <option value="Delivered">Delivered</option>
                    <option value="Received">Received</option>
                    <option value="Lost">Lost</option>
                  </select>
                </div>
                <div className="col-md-3">
                  <label>Priority</label>
                  <select
                    className="form-control"
                    value={filters.priority}
                    onChange={(e) => setFilters({ ...filters, priority: e.target.value })}
                  >
                    <option value="">All</option>
                    <option value="High">High</option>
                    <option value="Medium">Medium</option>
                    <option value="Low">Low</option>
                  </select>
                </div>
                <div className="col-md-3">
                  <label>Sort By</label>
                  <select
                    className="form-control"
                    value={filters.sortBy}
                    onChange={(e) => setFilters({ ...filters, sortBy: e.target.value })}
                  >
                    <option value="createdAt">Date Created</option>
                    <option value="price">Price</option>
                  </select>
                </div>
                <div className="col-md-3">
                  <label>Order</label>
                  <select
                    className="form-control"
                    value={filters.order}
                    onChange={(e) => setFilters({ ...filters, order: e.target.value })}
                  >
                    <option value="desc">Descending</option>
                    <option value="asc">Ascending</option>
                  </select>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="row">
        <div className="col-12">
          <div className="dashboard-card card">
            <div className="card-header">
              <h5 className="mb-0">Recent Shipments</h5>
            </div>
            <div className="card-body">
              {loading ? (
                <div className="loading-spinner"></div>
              ) : error ? (
                <div className="alert alert-danger">{error}</div>
              ) : recentShipments.length > 0 ? (
                <div className="table-responsive">
                  <table className="shipments-table">
                    <thead>
                      <tr>
                        <th>Product</th>
                        <th>From</th>
                        <th>To</th>
                        <th>Receiver</th>
                        <th>Status</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {recentShipments.map((shipment) => (
                        <tr key={shipment._id}>
                          <td data-label="Product">{shipment.name}</td>
                          <td data-label="From">{shipment.From}</td>
                          <td data-label="To">{shipment.To}</td>
                          <td data-label="Receiver">{shipment.Receiver}</td>
                          <td data-label="Status">
                            <span className={getStatusClass(shipment.status)}>
                              {shipment.status || 'Pending'}
                            </span>
                          </td>
                          <td data-label="Action">
                            <button
                              className="btn-track"
                              onClick={() => {
                                navigate('/track-product');
                                localStorage.setItem('tempTrackingId', shipment._id);
                              }}
                            >
                              Track
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="empty-state">
                  <i className="bi bi-inbox"></i>
                  <p>No recent shipments found.</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default UserDashboard;
