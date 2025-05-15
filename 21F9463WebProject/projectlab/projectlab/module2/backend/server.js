import 'dotenv/config';
import express from 'express';
import mongoose from 'mongoose';
import cors from 'cors';
import axios from 'axios';
import { createServer } from 'http';
import { Server } from 'socket.io';

const requiredEnvVars = ['MONGO_URI', 'PORT', 'AUTH_SERVICE_URL', 'JWT_SECRET'];
const missingEnvVars = requiredEnvVars.filter((varName) => !process.env[varName]);
if (missingEnvVars.length > 0) {
  throw new Error(`Missing required environment variables: ${missingEnvVars.join(', ')}`);
}

const app = express();
const server = createServer(app);
const io = new Server(server, {
  cors: {
    origin: 'http://localhost:5173',
    methods: ['GET', 'POST'],
    credentials: true,
  },
});

const PORT = process.env.PORT || 3001;
const MONGO_URI = process.env.MONGO_URI;
const AUTH_SERVICE_URL = process.env.AUTH_SERVICE_URL;

app.use(cors({
  origin: 'http://localhost:5173',
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization'],
}));
app.use(express.json());

// Schema
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
  trackingNumber: { type: String, unique: true },
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
    if (!token) return res.status(401).json({ error: 'Missing token' });

    const response = await axios.get(`${AUTH_SERVICE_URL}/api/auth/verify`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (response.data.valid) {
      req.userId = response.data.userId;
      req.userRole = response.data.role;
      console.log('Token verified for user:', req.userId);
      next();
    } else {
      res.status(401).json({ error: 'Invalid token' });
    }
  } catch (error) {
    console.error('Authentication failed:', error.message);
    res.status(401).json({ error: 'Authentication failed' });
  }
};

const isAdmin = (req, res, next) => {
  if (req.userRole !== 'admin') {
    return res.status(403).json({ error: 'Admin access required' });
  }
  next();
};

// Socket.IO Authentication
io.use((socket, next) => {
  const token = socket.handshake.auth.token;
  if (!token) return next(new Error('Authentication error'));

  axios.get(`${AUTH_SERVICE_URL}/api/auth/verify`, {
    headers: { Authorization: `Bearer ${token}` },
  })
    .then((response) => {
      if (response.data.valid) {
        socket.userId = response.data.userId;
        console.log('Socket.IO: Token verified for user:', socket.userId);
        next();
      } else {
        next(new Error('Authentication error'));
      }
    })
    .catch((error) => {
      console.error('Socket.IO: Authentication failed:', error.message);
      next(new Error('Authentication error'));
    });
});

io.on('connection', (socket) => {
  console.log('New client connected:', socket.id, 'User:', socket.userId);
  socket.on('disconnect', () => {
    console.log('Client disconnected:', socket.id);
  });
});

// Routes
app.get('/products', authenticate, async (req, res) => {
  try {
    const products = await Product.find({ userId: req.userId });
    res.status(200).json(products);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/products/:id', authenticate, async (req, res) => {
  try {
    const product = await Product.findOne({ _id: req.params.id, userId: req.userId });
    if (!product) return res.status(404).json({ error: 'Product not found or unauthorized' });
    res.status(200).json(product);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/user/shipments', authenticate, async (req, res) => {
  try {
    const { status, priority, sortBy = 'createdAt', order = 'desc' } = req.query;
    const query = { userId: req.userId };
    if (status) query.status = status;
    if (priority) query.Priority = priority;
    const sort = {};
    sort[sortBy] = order === 'desc' ? -1 : 1;
    const shipments = await Product.find(query).sort(sort);
    res.status(200).json(shipments);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// New admin endpoint to fetch all shipments
app.get('/api/admin/shipments', authenticate, isAdmin, async (req, res) => {
  try {
    const { status, priority, sortBy = 'createdAt', order = 'desc' } = req.query;
    const query = {};
    if (status) query.status = status;
    if (priority) query.Priority = priority;
    const sort = {};
    sort[sortBy] = order === 'desc' ? -1 : 1;
    const shipments = await Product.find(query).sort(sort);
    console.log('Fetched shipments for admin:', shipments);
    res.status(200).json(shipments);
  } catch (err) {
    console.error('Get admin shipments error:', err);
    res.status(500).json({ error: err.message });
  }
});

app.post('/ship', authenticate, async (req, res) => {
  try {
    const {
      name, category, description, price,
      Sender, Receiver, To, From, Priority, Address,
      toCoordinates, fromCoordinates,
    } = req.body;

    if (!name) throw new Error('Shipment name is required');
    if (!price || isNaN(price) || Number(price) <= 0) throw new Error('Price must be a positive number');
    if (!Sender) throw new Error('Sender is required');
    if (!Receiver) throw new Error('Receiver is required');
    if (!To) throw new Error('To location is required');
    if (!From) throw new Error('From location is required');
    if (!Priority) throw new Error('Priority is required');
    if (!Address) throw new Error('Address is required');

    const generateTrackingNumber = () => {
      const timestamp = Date.now();
      const random = Math.floor(Math.random() * 10000);
      return `TRK-${timestamp}-${random}`;
    };

    const product = new Product({
      name,
      category,
      description,
      price: Number(price),
      Sender,
      Receiver,
      To,
      From,
      Priority,
      Address,
      trackingNumber: generateTrackingNumber(),
      status: 'Pending',
      userId: req.userId,
      toCoordinates,
      fromCoordinates,
    });

    await product.save();
    io.emit(`statusUpdate:${req.userId}`, { shipmentId: product._id, status: 'Pending' });
    res.status(201).json({ message: 'Shipment created', id: product._id });
  } catch (err) {
    console.error('Error creating shipment:', err.message);
    if (err.code === 11000) {
      res.status(400).json({ error: 'Duplicate shipment detected. Please try again.' });
    } else {
      res.status(400).json({ error: err.message });
    }
  }
});

app.put('/products/:id', authenticate, async (req, res) => {
  try {
    const product = await Product.findOneAndUpdate(
      { _id: req.params.id, userId: req.userId },
      req.body,
      { new: true }
    );
    if (!product) return res.status(404).json({ error: 'Product not found or unauthorized' });

    if (req.body.status) {
      io.emit(`statusUpdate:${req.userId}`, { shipmentId: req.params.id, status: req.body.status });
    }

    res.json(product);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.delete('/products/:id', authenticate, async (req, res) => {
  try {
    const product = await Product.findOneAndDelete({ _id: req.params.id, userId: req.userId });
    if (!product) return res.status(404).json({ error: 'Product not found or unauthorized' });
    res.json({ message: 'Product deleted' });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.get('/', (req, res) => {
  res.send('Shipment Service is running...');
});

mongoose.connect(MONGO_URI)
  .then(() => {
    console.log('✅ Connected to MongoDB (ShipmentDB)');
    server.listen(PORT, () => {
      console.log(`🚀 Shipment Service running on http://localhost:${PORT}`);
    });
  })
  .catch((err) => {
    console.error('❌ Error connecting to MongoDB:', err);
    process.exit(1);
  });

export default app;