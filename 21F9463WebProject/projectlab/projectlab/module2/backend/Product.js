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
  trackingNumber: { type: String, unique: true }, // ✅ new field
  status: { type: String, default: 'Pending' },
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
  toCoordinates: { lat: Number, lng: Number },
  fromCoordinates: { lat: Number, lng: Number },
}, { timestamps: true });
