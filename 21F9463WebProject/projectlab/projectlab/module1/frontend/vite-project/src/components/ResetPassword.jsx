import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';

function ResetPassword() {
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get('token');

  // Log environment variables for debugging
  console.log('Environment variables:', import.meta.env);
  console.log('Reset Password Token:', token);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    setError('');

    // Validate inputs
    if (!token) {
      setError('Reset token is missing. Please use a valid reset link.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    if (newPassword.length < 6) {
      setError('Password must be at least 6 characters long');
      return;
    }

    setLoading(true);
    try {
      const baseUrl = import.meta.env.VITE_API_URL_AUTH || 'http://localhost:5000';
      const resetPasswordUrl = `${baseUrl}/api/auth/reset-password`;
      console.log('Reset Password URL:', resetPasswordUrl);
      console.log('Request Data:', { token, newPassword });
      const response = await axios.post(resetPasswordUrl, {
        token,
        newPassword,
      });
      console.log('Reset Password response:', response.data);
      setMessage(response.data.message);
      setTimeout(() => navigate('/'), 3000);
    } catch (err) {
      console.error('Reset Password error:', err.response || err);
      setError(err.response?.data?.error || 'Failed to reset password. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container mt-5">
      <h2>Reset Password</h2>
      {message && <div className="alert alert-success">{message}</div>}
      {error && <div className="alert alert-danger">{error}</div>}
      <form onSubmit={handleSubmit}>
        <div className="mb-3">
          <label className="form-label">New Password</label>
          <input
            type="password"
            className="form-control"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
            placeholder="Enter new password"
          />
        </div>
        <div className="mb-3">
          <label className="form-label">Confirm Password</label>
          <input
            type="password"
            className="form-control"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
            placeholder="Confirm new password"
          />
        </div>
        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? 'Resetting...' : 'Reset Password'}
        </button>
      </form>
      <p className="mt-3">
        <Link to="/">Back to Login</Link>
      </p>
    </div>
  );
}

export default ResetPassword;