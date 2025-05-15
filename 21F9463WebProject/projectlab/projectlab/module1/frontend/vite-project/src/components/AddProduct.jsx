import React, { useState, useCallback, useEffect } from 'react';
import axios from 'axios';
import { GoogleMap, Marker, LoadScript } from '@react-google-maps/api';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import './styles.css';

const mapContainerStyle = {
  width: '100%',
  height: '400px',
};

const defaultCenter = {
  lat: 37.7749,
  lng: -122.4194,
};

const libraries = ["places"];

const AddProduct = () => {
  const [newProduct, setNewProduct] = useState({
    name: '',
    price: '',
    Sender: '',
    Receiver: '',
    To: '',
    From: '',
    Priority: '',
    Address: '',
    toCoordinates: null,
    fromCoordinates: null,
  });
  const [mapType, setMapType] = useState(null);
  const [errors, setErrors] = useState({});
  const [mapKey, setMapKey] = useState(Date.now()); // Add a key to force re-render
  const [mapLoaded, setMapLoaded] = useState(false);
  
  // Handle map loading status
  useEffect(() => {
    // Reset map loaded status when map type changes
    if (mapType) {
      setMapLoaded(false);
      // Add a slight delay before setting mapLoaded to true
      const timer = setTimeout(() => setMapLoaded(true), 100);
      return () => clearTimeout(timer);
    }
  }, [mapType, mapKey]);

  const validateForm = () => {
    const newErrors = {};
    if (!newProduct.name) newErrors.name = 'Name is required';
    if (!newProduct.price || isNaN(newProduct.price) || Number(newProduct.price) <= 0)
      newErrors.price = 'Price must be a positive number';
    if (!newProduct.Sender) newErrors.Sender = 'Sender is required';
    if (!newProduct.Receiver) newErrors.Receiver = 'Receiver is required';
    if (!newProduct.To) newErrors.To = 'To location is required';
    if (!newProduct.From) newErrors.From = 'From location is required';
    if (!newProduct.Priority) newErrors.Priority = 'Priority is required';
    if (!newProduct.Address) newErrors.Address = 'Address is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleNewProductChange = (event) => {
    const { name, value } = event.target;
    setNewProduct((prev) => ({
      ...prev,
      [name]: value,
      ...(name === 'To' && !value ? { toCoordinates: null } : {}),
      ...(name === 'From' && !value ? { fromCoordinates: null } : {}),
    }));
    setErrors((prev) => ({ ...prev, [name]: '' }));
  };

  // Use useCallback to memoize the click handler
  const handleMapClick = useCallback((event) => {
    const lat = event.latLng.lat();
    const lng = event.latLng.lng();
    if (mapType === 'to') {
      setNewProduct((prev) => ({
        ...prev,
        To: `${lat}, ${lng}`,
        toCoordinates: { lat, lng },
      }));
      setErrors((prev) => ({ ...prev, To: '' }));
    } else if (mapType === 'from') {
      setNewProduct((prev) => ({
        ...prev,
        From: `${lat}, ${lng}`,
        fromCoordinates: { lat, lng },
      }));
      setErrors((prev) => ({ ...prev, From: '' }));
    }
    // Don't reset mapType here to keep the map visible
  }, [mapType]);

  const handleToggleMap = (type) => {
    // Force a re-render of the map when changing types
    setMapKey(Date.now());
    setMapType(prev => prev === type ? null : type);
  };

  const handleCloseMap = () => {
    setMapType(null);
  };

  const handleAddProduct = async () => {
    if (!validateForm()) {
      toast.error('Please fill all required fields correctly', { autoClose: 3000 });
      return;
    }
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        toast.error('Please log in to add a shipment', { autoClose: 3000 });
        return;
      }
      console.log('Sending shipment data:', newProduct);
      const response = await axios.post(
        `${import.meta.env.VITE_API_URL_SHIPMENT}/ship`,
        newProduct,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      toast.success('Shipment added successfully!', { autoClose: 3000 });
      setNewProduct({
        name: '',
        price: '',
        Sender: '',
        Receiver: '',
        To: '',
        From: '',
        Priority: '',
        Address: '',
        toCoordinates: null,
        fromCoordinates: null,
      });
      setErrors({});
    } catch (error) {
      console.error('Error adding product:', error);
      let errorMsg = error.response?.data?.error || 'Error adding shipment';
      if (errorMsg.includes('E11000') || errorMsg.includes('duplicate')) {
        errorMsg = 'A shipment with similar data already exists. Please use unique details.';
      }
      toast.error(errorMsg, { autoClose: 3000 });
    }
  };

  return (
    <div className="container mt-5">
      <ToastContainer pauseOnFocusLoss={false} pauseOnHover={false} hideProgressBar={true} />
      <h1 style={{ color: '#29f017', textAlign: 'center' }}>Add New Shipment</h1>
      <div className="card p-4">
        <h2>Add New Shipment</h2>
        <div className="form-group">
          <input
            type="text"
            name="name"
            placeholder="Shipment Name"
            value={newProduct.name}
            onChange={handleNewProductChange}
            className={`form-control ${errors.name ? 'is-invalid' : ''}`}
          />
          {errors.name && <div className="invalid-feedback">{errors.name}</div>}
        </div>
        <div className="form-group">
          <input
            type="number"
            name="price"
            placeholder="Price"
            value={newProduct.price}
            onChange={handleNewProductChange}
            className={`form-control ${errors.price ? 'is-invalid' : ''}`}
          />
          {errors.price && <div className="invalid-feedback">{errors.price}</div>}
        </div>
        <div className="form-group">
          <input
            type="text"
            name="Sender"
            placeholder="Sender Name"
            value={newProduct.Sender}
            onChange={handleNewProductChange}
            className={`form-control ${errors.Sender ? 'is-invalid' : ''}`}
          />
          {errors.Sender && <div className="invalid-feedback">{errors.Sender}</div>}
        </div>
        <div className="form-group">
          <input
            type="text"
            name="Receiver"
            placeholder="Receiver Name"
            value={newProduct.Receiver}
            onChange={handleNewProductChange}
            className={`form-control ${errors.Receiver ? 'is-invalid' : ''}`}
          />
          {errors.Receiver && <div className="invalid-feedback">{errors.Receiver}</div>}
        </div>
        <div className="form-group">
          <input
            type="text"
            name="To"
            placeholder="To (enter manually or select on map)"
            value={newProduct.To}
            onChange={handleNewProductChange}
            className={`form-control ${errors.To ? 'is-invalid' : ''}`}
          />
          {errors.To && <div className="invalid-feedback">{errors.To}</div>}
          <button
            className="btn btn-secondary mt-2"
            onClick={() => handleToggleMap('to')}
          >
            {mapType === 'to' ? 'Hide Map' : 'Select To Location on Map'}
          </button>
        </div>
        <div className="form-group">
          <input
            type="text"
            name="From"
            placeholder="From (enter manually or select on map)"
            value={newProduct.From}
            onChange={handleNewProductChange}
            className={`form-control ${errors.From ? 'is-invalid' : ''}`}
          />
          {errors.From && <div className="invalid-feedback">{errors.From}</div>}
          <button
            className="btn btn-secondary mt-2"
            onClick={() => handleToggleMap('from')}
          >
            {mapType === 'from' ? 'Hide Map' : 'Select From Location on Map'}
          </button>
        </div>
        <div className="form-group">
          <select
            name="Priority"
            value={newProduct.Priority}
            onChange={handleNewProductChange}
            className={`form-control ${errors.Priority ? 'is-invalid' : ''}`}
          >
            <option value="">Select Priority</option>
            <option value="High">High</option>
            <option value="Medium">Medium</option>
            <option value="Low">Low</option>
          </select>
          {errors.Priority && <div className="invalid-feedback">{errors.Priority}</div>}
        </div>
        <div className="form-group">
          <input
            type="text"
            name="Address"
            placeholder="Address"
            value={newProduct.Address}
            onChange={handleNewProductChange}
            className={`form-control ${errors.Address ? 'is-invalid' : ''}`}
          />
          {errors.Address && <div className="invalid-feedback">{errors.Address}</div>}
        </div>
        <button
          className="btn btn-primary mt-3"
          onClick={handleAddProduct}
        >
          Save Shipment
        </button>
      </div>
      {mapType && (
        <div className="mt-4">
          <LoadScript
            googleMapsApiKey={import.meta.env.VITE_GOOGLE_MAPS_API_KEY || ""}
            libraries={libraries}
            onLoad={() => console.log("Google Maps script loaded successfully")}
            onError={(error) => console.error("Error loading Google Maps script:", error)}
          >
            {mapLoaded && (
              <GoogleMap
                key={mapKey}
                mapContainerStyle={mapContainerStyle}
                center={
                  (mapType === 'to' && newProduct.toCoordinates) || 
                  (mapType === 'from' && newProduct.fromCoordinates) || 
                  defaultCenter
                }
                zoom={10}
                onClick={handleMapClick}
                onLoad={() => console.log("Map loaded successfully")}
              >
                {mapType === 'to' && newProduct.toCoordinates && (
                  <Marker position={newProduct.toCoordinates} />
                )}
                {mapType === 'from' && newProduct.fromCoordinates && (
                  <Marker position={newProduct.fromCoordinates} />
                )}
              </GoogleMap>
            )}
          </LoadScript>
          
          <div className="d-flex mt-2">
            <button 
              className="btn btn-secondary me-2" 
              onClick={handleCloseMap}
            >
              Close Map
            </button>
            <button 
              className="btn btn-success" 
              onClick={() => {
                if (mapType === 'to' && newProduct.toCoordinates) {
                  setMapType(null);
                } else if (mapType === 'from' && newProduct.fromCoordinates) {
                  setMapType(null);
                } else {
                  toast.info('Please select a location on the map first', { autoClose: 2000 });
                }
              }}
            >
              Confirm Selected Location
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default AddProduct;