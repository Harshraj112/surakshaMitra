// const mongoose = require("mongoose");
import mongoose from "mongoose";

const deviceSchema = new mongoose.Schema({
  userId: {
     type: mongoose.Schema.Types.ObjectId,
     ref: "User", required: true 
    },

  deviceId: {
    type: String,
    required: true,
    unique: true },
    pairedAt: {
         type: Date, 
         default: Date.now 
    },

  status: {
    type: String, // 'connected', 'disconnected'
    // required: true
  },

  triggeredAt: {
    type: Date,
    default: Date.now
  },

  signalStrength: {
  type: Number,
  // required: true,
  min: [-100, 'Signal strength must be at least -100'],
  max: [100, 'Signal strength must be at most 100']
}


});

const Device = mongoose.models.Device || mongoose.model("Device", deviceSchema);

export default Device;
export { deviceSchema };
