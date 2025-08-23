import mongoose from "mongoose";

const emergencyLogSchema = new mongoose.Schema({
  userId: {
     type: mongoose.Schema.Types.ObjectId, 
     ref: "User", required: true 
    },

  location: {
    lat: { type: Number, required: true },
    lng: { type: Number, required: true }
  },

  triggerType: {
     type: String, 
     enum: ["button", "voice", "panic"], 
     required: true 
    },

  voiceClipUrl: { type: String },

  time: {
    type: Date, 
    default: Date.now 
}
});

// module.exports = mongoose.model("EmergencyLog", emergencyLogSchema);

const EmergencyLog = mongoose.model("EmergencyLog", emergencyLogSchema);

export default EmergencyLog;
export { emergencyLogSchema };