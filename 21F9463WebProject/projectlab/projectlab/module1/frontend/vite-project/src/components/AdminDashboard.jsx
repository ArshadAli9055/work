import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { Bar } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend } from 'chart.js';
import './AdminDashboard.css';

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend);

function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('users');
  const [users, setUsers] = useState([]);
  const [shipments, setShipments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [adminProfile, setAdminProfile] = useState({});
  const [updateProfile, setUpdateProfile] = useState({
    name: '',
    email: '',
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    console.log('Shipments state updated:', shipments);
  }, [shipments]);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');
    console.log('AdminDashboard role check:', { role, token });

    if (!token || role !== 'admin') {
      alert('Unauthorized! Admins only.');
      navigate('/');
    } else {
      if (activeTab === 'users') {
        fetchUsers();
      } else if (activeTab === 'orders') {
        console.log('Fetching shipments...');
        fetchShipments();
      } else if (activeTab === 'settings') {
        fetchAdminProfile();
      }
    }
  }, [activeTab, navigate]);

  const fetchUsers = async (query = '') => {
    try {
      setLoading(true);
      setError('');
      const token = localStorage.getItem('token');
      const baseUrl = import.meta.env.VITE_API_URL_AUTH || 'http://localhost:5000';
      const endpoint = query
        ? `${baseUrl}/api/admin/users/search?query=${encodeURIComponent(query)}`
        : `${baseUrl}/api/admin/users`;
      const response = await axios.get(endpoint, {
        headers: { Authorization: `Bearer ${token}` },
      });
      console.log('Fetch users response:', response.data);
      setUsers(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error('Fetch users error:', err.response || err);
      if (err.response?.status === 401 || err.response?.status === 403) {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        navigate('/');
      }
      setError(err.response?.data?.error || 'Failed to load users');
      setUsers([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchShipments = async () => {
    try {
      setLoading(true);
      setError('');
      const token = localStorage.getItem('token');
      console.log('Token for fetchShipments:', token);
      if (!token) {
        throw new Error('No token found in localStorage');
      }
      const baseUrl = import.meta.env.VITE_API_URL_SHIPMENT || 'http://localhost:3001';
      console.log('Fetching shipments from:', `${baseUrl}/api/user/shipments`);
      const response = await axios.get(`${baseUrl}/api/user/shipments`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      console.log('Fetch shipments response:', response.data);
      if (!Array.isArray(response.data)) {
        console.warn('Response data is not an array:', response.data);
        setShipments([]);
      } else {
        setShipments(response.data);
      }
    } catch (err) {
      console.error('Fetch shipments error:', err.response?.data || err.message || err);
      if (err.response?.status === 401 || err.response?.status === 403) {
        console.log('Unauthorized or Forbidden - Redirecting to login...');
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        navigate('/');
      }
      setError(err.response?.data?.error || err.message || 'Failed to load shipments');
      setShipments([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchAdminProfile = async () => {
    try {
      setLoading(true);
      setError('');
      const token = localStorage.getItem('token');
      const baseUrl = import.meta.env.VITE_API_URL_AUTH || 'http://localhost:5000';
      const response = await axios.get(`${baseUrl}/api/admin/profile`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      console.log('Fetch admin profile response:', response.data);
      setAdminProfile(response.data);
      setUpdateProfile({
        name: response.data.name || '',
        email: response.data.email || '',
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      });
    } catch (err) {
      console.error('Fetch admin profile error:', err.response || err);
      if (err.response?.status === 401 || err.response?.status === 403) {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        navigate('/');
      }
      setError(err.response?.data?.error || 'Failed to load admin profile');
    } finally {
      setLoading(false);
    }
  };

  const updateOrderStatus = async (shipmentId, newStatus) => {
    try {
      setLoading(true);
      setError('');
      const token = localStorage.getItem('token');
      const baseUrl = import.meta.env.VITE_API_URL_SHIPMENT || 'http://localhost:3001';
      const response = await axios.put(
        `${baseUrl}/products/${shipmentId}`,
        { status: newStatus },
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }
      );
      if (response.status === 200) {
        setShipments(shipments.map((shipment) =>
          shipment._id === shipmentId ? { ...shipment, status: newStatus } : shipment
        ));
        alert('Shipment status updated successfully!');
      }
    } catch (err) {
      console.error('Update shipment status error:', err.response || err);
      if (err.response?.status === 401 || err.response?.status === 403) {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        navigate('/');
      }
      alert(err.response?.data?.error || 'Failed to update status');
    } finally {
      setLoading(false);
    }
  };

  const updateAdminAccount = async (e) => {
    e.preventDefault();
    if (updateProfile.newPassword) {
      if (updateProfile.newPassword !== updateProfile.confirmPassword) {
        alert('New passwords do not match!');
        return;
      }
      if (!updateProfile.currentPassword) {
        alert('Current password is required to set a new password!');
        return;
      }
    }
    try {
      setLoading(true);
      setError('');
      const token = localStorage.getItem('token');
      const baseUrl = import.meta.env.VITE_API_URL_AUTH || 'http://localhost:5000';
      const updateData = {
        name: updateProfile.name,
        email: updateProfile.email,
      };
      if (updateProfile.newPassword) {
        updateData.currentPassword = updateProfile.currentPassword;
        updateData.newPassword = updateProfile.newPassword;
      }
      const response = await axios.put(
        `${baseUrl}/api/admin/profile`,
        updateData,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }
      );
      if (response.status === 200) {
        alert('Profile updated successfully!');
        setAdminProfile({ ...adminProfile, name: updateProfile.name, email: updateProfile.email });
        setUpdateProfile({
          ...updateProfile,
          currentPassword: '',
          newPassword: '',
          confirmPassword: '',
        });
      }
    } catch (err) {
      console.error('Update admin profile error:', err.response || err);
      if (err.response?.status === 401) {
        alert('Current password is incorrect!');
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        navigate('/');
      } else if (err.response?.status === 403) {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        navigate('/');
      } else {
        alert(err.response?.data?.error || 'Failed to update profile');
      }
    } finally {
      setLoading(false);
    }
  };

  const toggleUserSelection = (userId) => {
    if (selectedUsers.includes(userId)) {
      setSelectedUsers(selectedUsers.filter((id) => id !== userId));
    } else {
      setSelectedUsers([...selectedUsers, userId]);
    }
  };

  const deleteSelectedUsers = async () => {
    if (selectedUsers.length === 0) {
      alert('No users selected!');
      return;
    }
    if (!window.confirm(`Are you sure you want to delete ${selectedUsers.length} selected user(s)?`)) {
      return;
    }
    try {
      setLoading(true);
      setError('');
      const token = localStorage.getItem('token');
      const baseUrl = import.meta.env.VITE_API_URL_AUTH || 'http://localhost:5000';
      const response = await axios.delete(`${baseUrl}/api/admin/users`, {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        data: { userIds: selectedUsers },
      });
      if (response.status === 200) {
        alert('Users deleted successfully!');
        setUsers(users.filter((user) => !selectedUsers.includes(user._id)));
        setSelectedUsers([]);
      }
    } catch (err) {
      console.error('Delete users error:', err.response || err);
      if (err.response?.status === 401 || err.response?.status === 403) {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        navigate('/');
      }
      alert(err.response?.data?.message || 'Failed to delete users');
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    navigate('/');
  };

  const getUserRoleClass = (role) => {
    return role === 'admin' ? 'user-role admin' : 'user-role user';
  };

  const handleSearch = () => {
    fetchUsers(searchTerm);
  };

  const statusCounts = shipments.reduce((acc, shipment) => {
    acc[shipment.status] = (acc[shipment.status] || 0) + 1;
    return acc;
  }, {});

  const chartData = {
    labels: Object.keys(statusCounts),
    datasets: [
      {
        label: 'Shipment Status',
        data: Object.values(statusCounts),
        backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0'],
      },
    ],
  };

  const renderContent = () => {
    if (loading) return <div className="admin-loading">Loading...</div>;
    if (error) return <div className="admin-error">{error}</div>;

    switch (activeTab) {
      case 'users':
        return (
          <>
            <h4>Manage Users</h4>
            <div className="user-actions">
              <input
                type="text"
                placeholder="Search users..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                className="search-input"
              />
              <button onClick={handleSearch}>Search</button>
            </div>
            {Array.isArray(users) && users.length > 0 ? (
              <ul className="user-list">
                {users.map((user) => (
                  <li key={user._id} className="user-item">
                    <span className="user-email">{user.email}</span>
                    <span className={getUserRoleClass(user.role)}>{user.role}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <div className="no-data">No users found.</div>
            )}
          </>
        );
      case 'settings':
        return (
          <>
            <h4>Settings</h4>
            <div className="settings-panel">
              <div className="settings-section">
                <h5 className="settings-heading">User Management</h5>
                <div className="user-actions mb-3">
                  <input
                    type="text"
                    placeholder="Search users..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                    className="search-input"
                  />
                  <button
                    className="btn-delete-users"
                    onClick={deleteSelectedUsers}
                    disabled={selectedUsers.length === 0}
                  >
                    Delete Selected ({selectedUsers.length})
                  </button>
                </div>
                <div className="user-management-table">
                  {Array.isArray(users) && users.length > 0 ? (
                    <table className="users-table">
                      <thead>
                        <tr>
                          <th>Select</th>
                          <th>Email</th>
                          <th>Role</th>
                        </tr>
                      </thead>
                      <tbody>
                        {users.map((user) => (
                          <tr key={user._id}>
                            <td>
                              <input
                                type="checkbox"
                                checked={selectedUsers.includes(user._id)}
                                onChange={() => toggleUserSelection(user._id)}
                                disabled={user.role === 'admin'}
                              />
                            </td>
                            <td>{user.email}</td>
                            <td>{user.role}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  ) : (
                    <div className="no-data">No users found.</div>
                  )}
                </div>
              </div>

              <div className="settings-section mt-4">
                <h5 className="settings-heading">Admin Account Management</h5>
                <form onSubmit={updateAdminAccount} className="admin-profile-form">
                  <div className="form-group">
                    <label>Name</label>
                    <input
                      type="text"
                      value={updateProfile.name}
                      onChange={(e) => setUpdateProfile({ ...updateProfile, name: e.target.value })}
                      className="form-control"
                    />
                  </div>
                  <div className="form-group">
                    <label>Email</label>
                    <input
                      type="email"
                      value={updateProfile.email}
                      onChange={(e) => setUpdateProfile({ ...updateProfile, email: e.target.value })}
                      className="form-control"
                    />
                  </div>
                  <hr />
                  <h6>Change Password</h6>
                  <div className="form-group">
                    <label>Current Password</label>
                    <input
                      type="password"
                      value={updateProfile.currentPassword}
                      onChange={(e) => setUpdateProfile({ ...updateProfile, currentPassword: e.target.value })}
                      className="form-control"
                    />
                  </div>
                  <div className="form-group">
                    <label>New Password</label>
                    <input
                      type="password"
                      value={updateProfile.newPassword}
                      onChange={(e) => setUpdateProfile({ ...updateProfile, newPassword: e.target.value })}
                      className="form-control"
                    />
                  </div>
                  <div className="form-group">
                    <label>Confirm New Password</label>
                    <input
                      type="password"
                      value={updateProfile.confirmPassword}
                      onChange={(e) => setUpdateProfile({ ...updateProfile, confirmPassword: e.target.value })}
                      className="form-control"
                    />
                  </div>
                  <button type="submit" className="btn-update-profile">Update Profile</button>
                </form>
              </div>

              <div className="settings-section mt-4">
                <h5 className="settings-heading">System Settings</h5>
                <p className="settings-coming-soon">Additional system settings coming soon...</p>
              </div>
            </div>
          </>
        );
      case 'orders':
        return (
          <>
            <h4>Manage Product Status</h4>
            <Bar
              data={chartData}
              options={{
                responsive: true,
                plugins: {
                  legend: { position: 'top' },
                  title: { display: true, text: 'Shipment Status Distribution' },
                },
                scales: {
                  y: {
                    beginAtZero: true,
                    ticks: {
                      stepSize: 1,
                    },
                  },
                },
              }}
            />
            {Array.isArray(shipments) && shipments.length > 0 ? (
              <table className="orders-table">
                <thead>
                  <tr>
                    <th>Product ID</th>
                    <th>Name</th>
                    <th>Price</th>
                    <th>Sender</th>
                    <th>Receiver</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {shipments.map((shipment) => (
                    <tr key={shipment._id}>
                      <td data-label="Product ID">{shipment._id}</td>
                      <td data-label="Name">{shipment.name || 'N/A'}</td>
                      <td data-label="Price">{shipment.price ? `$${shipment.price}` : 'N/A'}</td>
                      <td data-label="Sender">{shipment.Sender || 'N/A'}</td>
                      <td data-label="Receiver">{shipment.Receiver || 'N/A'}</td>
                      <td data-label="Status">{shipment.status}</td>
                      <td data-label="Action">
                        <select
                          className="status-select"
                          value={shipment.status}
                          onChange={(e) => updateOrderStatus(shipment._id, e.target.value)}
                        >
                          <option value="Pending">Pending</option>
                          <option value="Delivered">Delivered</option>
                        </select>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="no-data">No shipments found.</div>
            )}
          </>
        );
      default:
        return null;
    }
  };

  return (
    <div className="admin-container container mt-5">
      <div className="admin-header">
        <h2 className="admin-title">Admin Dashboard</h2>
      </div>

      <div className="mt-4">
        <ul className="nav nav-tabs admin-tabs">
          <li className="nav-item">
            <button
              className={`nav-link ${activeTab === 'users' ? 'active' : ''}`}
              onClick={() => setActiveTab('users')}
            >
              Users
            </button>
          </li>
          <li className="nav-item">
            <button
              className={`nav-link ${activeTab === 'settings' ? 'active' : ''}`}
              onClick={() => setActiveTab('settings')}
            >
              Settings
            </button>
          </li>
          <li className="nav-item">
            <button
              className={`nav-link ${activeTab === 'orders' ? 'active' : ''}`}
              onClick={() => setActiveTab('orders')}
            >
              Product Status
            </button>
          </li>
        </ul>

        <div className="admin-content">{renderContent()}</div>

        <div className="text-end mt-3">
          <button onClick={logout} className="btn-admin-logout">Logout</button>
        </div>
      </div>
    </div>
  );
}

export default AdminDashboard;