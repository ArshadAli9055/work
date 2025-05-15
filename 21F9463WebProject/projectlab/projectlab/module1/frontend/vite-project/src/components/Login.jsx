import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { GoogleLogin, GoogleOAuthProvider } from '@react-oauth/google';

function Login() {
  const [credentials, setCredentials] = useState({
    email: '',
    password: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  console.log('Google Client ID:', import.meta.env.VITE_GOOGLE_CLIENT_ID);

  const handleChange = (e) => {
    setCredentials({
      ...credentials,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const baseUrl = import.meta.env.VITE_API_URL_AUTH || 'http://localhost:5000';
      const loginUrl = `${baseUrl}/api/auth/login`;
      console.log('Login URL:', loginUrl);
      console.log('Credentials:', credentials);
      const response = await axios.post(loginUrl, credentials);
      console.log('Login response:', response.data);
      const { token, role, userId } = response.data;

      // Validate response
      if (!token || !userId || !role) {
        throw new Error('Invalid login response: missing token, userId, or role');
      }

      // Validate role
      const validRoles = ['user', 'admin'];
      if (!validRoles.includes(role)) {
        console.warn('Invalid role received:', role);
        throw new Error(`Invalid role: ${role}`);
      }

      // Store in localStorage
      localStorage.setItem('token', token);
      localStorage.setItem('role', role);
      localStorage.setItem('userId', userId);
      console.log('Stored localStorage:', { token, role, userId });

      // Navigate based on role
      if (role === 'admin') {
        console.log('Navigating to /admin-dashboard');
        navigate('/admin-dashboard', { replace: true });
      } else {
        console.log('Navigating to /dashboard');
        navigate('/dashboard', { replace: true });
      }
    } catch (err) {
      console.error('Login error:', err.response || err);
      setError(err.response?.data?.error || `Login failed: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSuccess = async (credentialResponse) => {
    setLoading(true);
    setError('');
    try {
      const baseUrl = import.meta.env.VITE_API_URL_AUTH || 'http://localhost:5000';
      const googleLoginUrl = `${baseUrl}/api/auth/google-login`;
      console.log('Google Login URL:', googleLoginUrl);
      console.log('Google Credential:', credentialResponse.credential);
      const response = await axios.post(googleLoginUrl, {
        token: credentialResponse.credential,
      });
      console.log('Google login response:', response.data);
      const { token, role, userId, name, email, picture } = response.data;

      // Validate response
      if (!token || !userId || !role) {
        throw new Error('Invalid Google login response: missing token, userId, or role');
      }

      // Validate role
      const validRoles = ['user', 'admin'];
      if (!validRoles.includes(role)) {
        console.warn('Invalid role received:', role);
        throw new Error(`Invalid role: ${role}`);
      }

      // Store in localStorage
      localStorage.setItem('token', token);
      localStorage.setItem('role', role);
      localStorage.setItem('name', name);
      localStorage.setItem('email', email);
      localStorage.setItem('picture', picture);
      localStorage.setItem('userId', userId);
      console.log('Stored localStorage:', { token, role, userId, name, email, picture });

      // Navigate based on role
      if (role === 'admin') {
        console.log('Navigating to /admin-dashboard');
        navigate('/admin-dashboard', { replace: true });
      } else {
        console.log('Navigating to /dashboard');
        navigate('/dashboard', { replace: true });
      }
    } catch (err) {
      console.error('Google login error:', err.response || err);
      setError(err.response?.data?.error || 'Google login failed');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleError = () => {
    console.error('Google login failed');
    setError('Google login failed');
  };

  return (
    <div className="container mt-5">
      <h2>Login</h2>
      {error && <div className="alert alert-danger">{error}</div>}
      <form onSubmit={handleSubmit}>
        <div className="mb-3">
          <label className="form-label">Email</label>
          <input
            type="email"
            className="form-control"
            name="email"
            value={credentials.email}
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
            value={credentials.password}
            onChange={handleChange}
            required
          />
        </div>
        <button type="submit" className="btn btn-success" disabled={loading}>
          {loading ? 'Logging in...' : 'Login'}
        </button>
      </form>
      <div className="mt-3">
        <p>Or login with Google:</p>
        <GoogleOAuthProvider clientId={import.meta.env.VITE_GOOGLE_CLIENT_ID}>
          <GoogleLogin
            onSuccess={handleGoogleSuccess}
            onError={handleGoogleError}
            disabled={loading}
            type="standard"
            theme="outline"
            size="large"
            text="signin_with"
            shape="rectangular"
            logo_alignment="left"
          />
        </GoogleOAuthProvider>
      </div>
      <p className="mt-3">
        Don't have an account? <Link to="/signup">Sign up here</Link>
      </p>
      <p>
        Forgotten your password? <Link to="/forgot-password">Reset it here</Link>
      </p>
    </div>
  );
}

export default Login;