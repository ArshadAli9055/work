import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { GoogleOAuthProvider, GoogleLogin } from '@react-oauth/google';

function Signup() {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: 'user',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const baseUrl = import.meta.env.VITE_API_URL_AUTH || 'http://localhost:5000';
      const signupUrl = `${baseUrl}/api/auth/signup`;
      console.log('Signup URL:', signupUrl);
      console.log('Form Data:', formData);
      const response = await axios.post(signupUrl, formData);
      console.log('Signup response:', response.data);
      alert('Signup successful! Please log in.');
      navigate('/');
    } catch (err) {
      console.error('Signup error:', err.response || err);
      setError(err.response?.data?.error || 'Signup failed');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSuccess = async (credentialResponse) => {
    setLoading(true);
    setError('');
    try {
      const baseUrl = import.meta.env.VITE_API_URL_AUTH || 'http://localhost:5000';
      const googleSignupUrl = `${baseUrl}/api/auth/google-login`;
      console.log('Google Signup URL:', googleSignupUrl);
      console.log('Google Credential:', credentialResponse.credential);
      const response = await axios.post(googleSignupUrl, {
        token: credentialResponse.credential,
      });
      console.log('Google signup response:', response.data);
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('role', response.data.role);
      localStorage.setItem('name', response.data.name);
      localStorage.setItem('email', response.data.email);
      localStorage.setItem('picture', response.data.picture);
      localStorage.setItem('userId', response.data.userId);
      if (response.data.role === 'admin') {
        navigate('/admin-dashboard');
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      console.error('Google signup error:', err.response || err);
      setError(err.response?.data?.error || 'Google signup failed');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleError = () => {
    console.error('Google signup failed');
    setError('Google signup failed');
  };

  return (
    <div className="container mt-5">
      <h2>Sign Up</h2>
      {error && <div className="alert alert-danger">{error}</div>}
      <form onSubmit={handleSubmit}>
        <div className="mb-3">
          <label className="form-label">Name</label>
          <input
            type="text"
            className="form-control"
            name="name"
            value={formData.name}
            onChange={handleChange}
            required
          />
        </div>
        <div className="mb-3">
          <label className="form-label">Email</label>
          <input
            type="email"
            className="form-control"
            name="email"
            value={formData.email}
            onChange={handleChange}
            required
          />
        </div>
        <div className="mb-3">
          <label className="form-label">Password</label>
          <input
            type="password"
            className="form-control"
            name="password"
            value={formData.password}
            onChange={handleChange}
            required
          />
        </div>
        <div className="mb-3">
          <label className="form-label">Confirm Password</label>
          <input
            type="password"
            className="form-control"
            name="confirmPassword"
            value={formData.confirmPassword}
            onChange={handleChange}
            required
          />
        </div>
        <div className="mb-3">
          <label className="form-label">Role</label>
          <select
            className="form-control"
            name="role"
            value={formData.role}
            onChange={handleChange}
          >
            <option value="user">User</option>
            <option value="admin">Admin</option>
          </select>
        </div>
        <button type="submit" className="btn btn-success" disabled={loading}>
          {loading ? 'Signing up...' : 'Sign Up'}
        </button>
      </form>
      <div className="mt-3">
        <p>Or sign up with Google:</p>
        <GoogleOAuthProvider clientId={import.meta.env.VITE_GOOGLE_CLIENT_ID}>
          <GoogleLogin
            onSuccess={handleGoogleSuccess}
            onError={handleGoogleError}
            disabled={loading}
            type="standard"
            theme="outline"
            size="large"
            text="signup_with"
            shape="rectangular"
            logo_alignment="left"
          />
        </GoogleOAuthProvider>
      </div>
      <p className="mt-3">
        Already have an account? <Link to="/">Log in here</Link>
      </p>
    </div>
  );
}

export default Signup;