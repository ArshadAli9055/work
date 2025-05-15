require("dotenv").config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const axios = require('axios');
const nodemailer = require('nodemailer');
const http = require('http');
const { Server } = require('socket.io');
const PDFDocument = require('pdfkit');

const requiredEnvVars = ['MONGO_URI', 'PORT', 'AUTH_SERVICE_URL', 'EMAIL_USER', 'EMAIL_PASS'];
const missingEnvVars = requiredEnvVars.filter((varName) => !process.env[varName]);
if (missingEnvVars.length > 0) {
  throw new Error(`Missing required environment variables: ${missingEnvVars.join(', ')}`);
}

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: 'http://localhost:5173',
    methods: ['GET', 'POST', 'PUT', 'DELETE'],
  },
});
const PORT = process.env.PORT || 3000;
const MONGO_URI = process.env.MONGO_URI;
const AUTH_SERVICE_URL = process.env.AUTH_SERVICE_URL;

// Configure CORS
app.use(cors({
  origin: 'http://localhost:5173',
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
app.use(express.json());

// Nodemailer setup
const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: process.env.EMAIL_USER,
    pass: process.env.EMAIL_PASS,
  },
});

// Schemas
const productSchema = new mongoose.Schema({
  name: { type: String, required: true },
  category: String,
  description: String,
  price: { type: Number, required: true, min: 0 },
  Sender: String,
  Receiver: String,
  To: String,
  From: String,
  Priority: String,
  Address: String,
  status: { type: String, default: 'Pending' },
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
  toCoordinates: { lat: Number, lng: Number },
  fromCoordinates: { lat: Number, lng: Number },
}, { timestamps: true });

const Product = mongoose.model('Product', productSchema);

// Authentication Middleware
const authenticate = async (req, res, next) => {
  try {
    const token = req.headers.authorization?.split(' ')[1];
    if (!token) {
      return res.status(401).json({ error: 'Missing token' });
    }
    const response = await axios.get(`${AUTH_SERVICE_URL}/api/auth/verify`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (response.data.valid) {
      req.userId = response.data.userId;
      req.userRole = response.data.role;
      next();
    } else {
      res.status(401).json({ error: 'Invalid token' });
    }
  } catch (error) {
    res.status(401).json({ error: 'Authentication failed' });
  }
};

// Socket.IO
io.on('connection', (socket) => {
  console.log('A user connected:', socket.id);
  socket.on('disconnect', () => {
    console.log('User disconnected:', socket.id);
  });
});

// Routes
app.get('/track/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const product = await Product.findById(id);
    if (!product) {
      return res.status(404).json({ message: 'Shipment not found' });
    }
    res.status(200).json(product);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/updateStatus/:id', authenticate, async (req, res) => {
  try {
    const { id } = req.params;
    const { status } = req.body;
    const product = await Product.findByIdAndUpdate(id, { status }, { new: true });
    if (!product) {
      return res.status(404).json({ message: 'Product not found' });
    }
    io.emit(`statusUpdate:${product.userId}`, { shipmentId: id, status });
    const userResponse = await axios.get(`${AUTH_SERVICE_URL}/api/admin/users`, {
      headers: { Authorization: req.headers.authorization },
    });
    const user = userResponse.data.find(u => u._id === product.userId.toString());
    if (user) {
      await transporter.sendMail({
        from: `"Shipment App" <${process.env.EMAIL_USER}>`,
        to: user.email,
        subject: 'Shipment Status Updated',
        html: `<p>Hello ${user.name},</p><p>Your shipment "${product.name}" has been updated to "${status}".</p>`,
      });
    }
    res.status(200).json(product);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/shipment/:id/pdf', async (req, res) => {
  try {
    const { id } = req.params;
    const product = await Product.findById(id);
    if (!product) {
      return res.status(404).json({ error: 'Shipment not found' });
    }
    const doc = new PDFDocument();
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `attachment; filename=shipment_${id}.pdf`);
    doc.pipe(res);
    doc.fontSize(20).text('Shipment Details', { align: 'center' });
    doc.moveDown();
    doc.fontSize(12).text(`Tracking Number: ${product._id}`);
    doc.text(`Name: ${product.name}`);
    doc.text(`Price: $${product.price}`);
    doc.text(`Sender: ${product.Sender}`);
    doc.text(`Receiver: ${product.Receiver}`);
    doc.text(`To: ${product.To}`);
    doc.text(`From: ${product.From}`);
    doc.text(`Priority: ${product.Priority}`);
    doc.text(`Address: ${product.Address}`);
    doc.text(`Status: ${product.status}`);
    doc.end();
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/', (req, res) => {
  res.send('Tracking Service is running...');
});

mongoose.connect(MONGO_URI)
  .then(() => {
    console.log('✅ Connected to MongoDB (TrackingDB)');
    server.listen(PORT, () => {
      console.log(`🚀 Tracking Service running on http://localhost:${PORT}`);
    });
  })
  .catch((err) => {
    console.error('❌ Error connecting to MongoDB:', err);
    process.exit(1);
  });

module.exports = app;