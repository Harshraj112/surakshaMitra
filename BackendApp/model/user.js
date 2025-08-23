import mongoose from "mongoose";
const Schema = mongoose.Schema;

const userSchema = new mongoose.Schema({
  fullname: {
    type: String,
    required: true,
    trim: true
  },

  email: {
    type: String,
    required: true,
    unique: true,
    lowercase: true
  },

  password: {
    type: String,
    required: true
  },

  phone: {
    type: String,
    required: true,
    unique: true
  },

  emergencyContacts: {
    type: [String],
    validate: {
      validator: function (value) {
        return (
          value.length >= 1 &&
          value.length <= 3 &&
          value.every((v) => /^[0-9]{10}$/.test(v))
        );
      },
      message:
        "You must provide between 1 and 3 emergency contact numbers, each 10 digits.",
    },
    required: true,
    default: undefined,
  },

  locationHistory: [
    {
      timestamp: { type: Date, default: Date.now },
      latitude: Number,
      longitude: Number
    }
  ],

  alerts: [
    {
      triggeredAt: {
        type: Date,
        default: Date.now,
      },
      type: {
        type: String,
        required: true,
      },
    },
  ],

  createdAt: {
    type: Date,
    default: Date.now
  },

  // ✅ OTP-related fields
  otp: {
    type: String
  },

  otpExpires: {
    type: Date
  },

  isVerified: {
    type: Boolean,
    default: false
  },

});

const User = mongoose.model("User", userSchema);

export default User;
export { userSchema };