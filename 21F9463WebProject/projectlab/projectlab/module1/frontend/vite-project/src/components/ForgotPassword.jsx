import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';

function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      const baseUrl = import.meta.env.VITE_API_URL_AUTH || 'http://localhost:5000';
      const forgotPasswordUrl = `${baseUrl}/api/auth/forgot-password`;
      console.log('Forgot Password URL:', forgotPasswordUrl);
      console.log('Email:', email);
      const response = await axios.post(forgotPasswordUrl, { email });
      console.log('Forgot Password response:', response.data);
      setSuccess('Password reset link sent to your email.');
      setEmail('');
    } catch (err) {
      console.error('Forgot Password error:', err.response || err);
      setError(err.response?.data?.error || 'Failed to send reset link');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container mt-5">
      <h2>Forgot Password</h2>
      {error && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}
      <form onSubmit={handleSubmit}>
        <div className="mb-3">
          <label className="form-label">Email</label>
          <input
            type="email"
            className="form-control"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>
        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? 'Sending...' : 'Send Reset Link'}
        </button>
      </form>
      <p className="mt-3">
        Remember your password? <Link to="/">Log in here</Link>
      </p>
      <p>
        Don't have an account? <Link to="/signup">Sign up here</Link>
      </p>
    </div>
  );
}

export default ForgotPassword;