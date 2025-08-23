// dbConnect.js
import mongoose from "mongoose";

const dbConnect = async () => {
  try {
    mongoose.set("strictQuery", false);

    console.log("🔗 Connecting to MongoDB:", process.env.MONGO_URL);

    const connected = await mongoose.connect(process.env.MONGO_URL, {
      tls: true,            // enforce TLS
      tlsAllowInvalidCertificates: false,
      tlsAllowInvalidHostnames: false,
      serverSelectionTimeoutMS: 5000, // fail fast if cannot connect
    });

    console.log(`✅ MongoDB connected: ${connected.connection.host}`);
  } catch (error) {
    console.error(`❌ MongoDB connection error: ${error.message}`);
    process.exit(1);
  }
};

export default dbConnect;
