import React, { useState } from 'react';
import axios from 'axios';
import { GoogleMap, LoadScript, Marker } from '@react-google-maps/api';

const mapContainerStyle = {
  width: '100%',
  height: '400px',
};

const defaultCenter = {
  lat: 37.7749,
  lng: -122.4194,
};

function ShipmentTracker() {
  const [trackingNumber, setTrackingNumber] = useState('');
  const [shipmentDetails, setShipmentDetails] = useState(null);
  const [error, setError] = useState('');

  const trackShipment = async () => {
    if (!trackingNumber.trim()) {
      setError('Please enter a tracking number');
      return;
    }
    try {
      const response = await axios.get(`${import.meta.env.VITE_API_URL_TRACKING}/track/${trackingNumber}`);
      if (response.status === 200) {
        setShipmentDetails(response.data);
        setError('');
      }
    } catch (error) {
      console.error('Error fetching shipment details:', error);
      setShipmentDetails(null);
      setError(error.response?.data?.error || 'Shipment not found or server error.');
    }
  };

  const handleStatusUpdate = async (newStatus) => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.put(
        `${import.meta.env.VITE_API_URL_TRACKING}/updateStatus/${trackingNumber}`,
        { status: newStatus },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      if (response.status === 200) {
        setShipmentDetails({ ...shipmentDetails, status: newStatus });
        setError('');
      }
    } catch (error) {
      console.error('Error updating shipment status:', error);
      setError(error.response?.data?.error || 'Could not update status.');
    }
  };

  const downloadPDF = async () => {
    try {
      const response = await axios.get(`${import.meta.env.VITE_API_URL_TRACKING}/api/shipment/${trackingNumber}/pdf`, {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `shipment_${trackingNumber}.pdf`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (error) {
      console.error('Error downloading PDF:', error);
      setError(error.response?.data?.error || 'Could not download PDF.');
    }
  };

  return (
    <div className="shipment-tracker">
      <h1>📦 Track Your Shipment</h1>
      <div className="tracking-form">
        <input
          type="text"
          value={trackingNumber}
          onChange={(e) => setTrackingNumber(e.target.value)}
          placeholder="Enter Tracking ID (ObjectID)"
        />
        <button onClick={trackShipment}>Track Shipment</button>
      </div>

      {error && <p className="error">{error}</p>}

      {shipmentDetails && (
        <div className="shipment-details">
          <h2>📋 Shipment Details</h2>
          <p><strong>Tracking Number:</strong> {shipmentDetails._id}</p>
          <p><strong>Name:</strong> {shipmentDetails.name}</p>
          <p><strong>Price:</strong> ${shipmentDetails.price}</p>
          <p><strong>Sender:</strong> {shipmentDetails.Sender}</p>
          <p><strong>Receiver:</strong> {shipmentDetails.Receiver}</p>
          <p><strong>To:</strong> {shipmentDetails.To}</p>
          <p><strong>From:</strong> {shipmentDetails.From}</p>
          <p><strong>Priority:</strong> {shipmentDetails.Priority}</p>
          <p><strong>Address:</strong> {shipmentDetails.Address}</p>

          <h2>🚚 Shipment Status</h2>
          <p><strong>Current Status:</strong> {shipmentDetails.status || 'Pending'}</p>

          <div className="status-buttons">
            <button onClick={() => handleStatusUpdate('Received')}>Mark as Received</button>
            <button onClick={() => handleStatusUpdate('Lost')}>Mark as Lost</button>
            <button onClick={downloadPDF}>Download PDF</button>
          </div>

          {(shipmentDetails.toCoordinates || shipmentDetails.fromCoordinates) && (
            <LoadScript googleMapsApiKey={import.meta.env.VITE_GOOGLE_MAPS_API_KEY}>
              <GoogleMap
                mapContainerStyle={mapContainerStyle}
                center={
                  shipmentDetails.toCoordinates || shipmentDetails.fromCoordinates || defaultCenter
                }
                zoom={10}
              >
                {shipmentDetails.toCoordinates && (
                  <Marker position={shipmentDetails.toCoordinates} label="To" />
                )}
                {shipmentDetails.fromCoordinates && (
                  <Marker position={shipmentDetails.fromCoordinates} label="From" />
                )}
              </GoogleMap>
            </LoadScript>
          )}
        </div>
      )}
    </div>
  );
}

export default ShipmentTracker;