import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { LoadScript } from '@react-google-maps/api';
import axios from 'axios';
import Login from './components/Login';
import Signup from './components/Signup';
import UserDashboard from './components/UserDashboard';
import AdminDashboard from './components/AdminDashboard';
import AddProduct from './components/AddProduct';
import TrackProduct from './components/TrackProduct';
import ForgotPassword from './components/ForgotPassword';
import ResetPassword from './components/ResetPassword';
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';

const ProtectedRoute = ({ children, requiredRole }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(null);
  const [userRole, setUserRole] = useState(null);
  const [errorMessage, setErrorMessage] = useState(localStorage.getItem('accessDeniedMessage') || '');

  useEffect(() => {
    const verifyToken = async () => {
      const token = localStorage.getItem('token');
      const storedRole = localStorage.getItem('role');
      console.log('ProtectedRoute verifying token:', { token, storedRole, requiredRole });

      if (!token) {
        console.warn('No token found, redirecting to login');
        setIsAuthenticated(false);
        return;
      }

      try {
        const baseUrl = import.meta.env.VITE_API_URL_AUTH || 'http://localhost:5000';
        const response = await axios.get(`${baseUrl}/api/auth/verify`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        console.log('Verify token response:', response.data);
        const { role, userId } = response.data;

        if (!['user', 'admin'].includes(role)) {
          throw new Error(`Invalid role received: ${role}`);
        }

        setIsAuthenticated(true);
        setUserRole(role);
        localStorage.setItem('userId', userId);
        localStorage.setItem('role', role);
        console.log('ProtectedRoute updated:', { isAuthenticated: true, userRole: role, userId });
      } catch (error) {
        console.error('Token verification failed:', error.response || error);
        setIsAuthenticated(false);
        setErrorMessage('Session expired or invalid. Please log in again.');
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        localStorage.removeItem('userId');
      }
    };
    verifyToken();
  }, []);

  useEffect(() => {
    if (errorMessage) {
      setTimeout(() => {
        localStorage.removeItem('accessDeniedMessage');
        setErrorMessage('');
      }, 5000);
    }
  }, [errorMessage]);

  if (isAuthenticated === null) {
    return <div className="container mt-5 text-center">Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  if (requiredRole && userRole !== requiredRole) {
    const message = `Access denied: ${requiredRole} role required.`;
    localStorage.setItem('accessDeniedMessage', message);
    console.warn('Role mismatch:', { userRole, requiredRole, redirectingTo: userRole === 'admin' ? '/admin-dashboard' : '/dashboard' });
    return <Navigate to={userRole === 'admin' ? '/admin-dashboard' : '/dashboard'} replace />;
  }

  return (
    <div>
      {errorMessage && (
        <div className="container mt-3">
          <div className="alert alert-danger alert-dismissible fade show" role="alert">
            {errorMessage}
            <button type="button" className="btn-close" onClick={() => setErrorMessage('')} aria-label="Close"></button>
          </div>
        </div>
      )}
      {children}
    </div>
  );
};

function App() {
  return (
    <LoadScript googleMapsApiKey={import.meta.env.VITE_GOOGLE_MAPS_API_KEY}>
      <Router>
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute requiredRole="user">
                <UserDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin-dashboard"
            element={
              <ProtectedRoute requiredRole="admin">
                <AdminDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/add-product"
            element={
              <ProtectedRoute requiredRole="user">
                <AddProduct />
              </ProtectedRoute>
            }
          />
          <Route
            path="/track-product"
            element={
              <ProtectedRoute requiredRole="user">
                <TrackProduct />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<div className="container mt-5"><h2>404 - Page Not Found</h2></div>} />
        </Routes>
      </Router>
    </LoadScript>
  );
}

export default App;