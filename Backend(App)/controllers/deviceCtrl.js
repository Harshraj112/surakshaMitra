import Device from "../model/device.js";
import User from "../model/user.js";

// @desc    Sync device with user
// @route   POST /api/device-sync
// @access  Public or Protected (based on auth)
export const syncDevice = async (req, res) => {
  const { deviceId, userId } = req.body;

  if (!deviceId || !userId) {
    return res.status(400).json({ message: "Device ID and User ID are required" });
  }

  try {
    let device = await Device.findOne({ deviceId });

    if (!device) {
      // Create new device
      device = new Device({ deviceId, userId });
      await device.save();
    } else {
      // Update existing device's user
      device.userId = userId;
      await device.save();
    }

    res.status(200).json({ message: "Device synced successfully", device });
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: "Server error" });
  }
};

// @desc    Send emergency contacts to the user
// @route   POST /api/emergency-contacts
// @access  Private
export const getEmergencyContacts = async (req, res) => {
  const { deviceId } = req.params;

  try {
    const user = await User.findById(deviceId).select("emergencyContacts");

    if (!user) {
      return res.status(404).json({ message: "Device/User not found" });
    }

    res.status(200).json(user.emergencyContacts);
  } catch (error) {
    console.error("Error fetching contacts:", error);
    res.status(500).json({ message: "Server error" });
  }
};



export const updateConnectivityStatus = async (req, res) => {
  const { userId, status, signalStrength } = req.body;

  if (!userId || !status) {
    return res.status(400).json({ message: "userId and status required" });
  }

  try {
    // Step 1: Find device by userId
    const device = await Device.findOne({ userId });

    if (!device) {
      return res.status(404).json({ message: "Device not found for this user" });
    }

    // Step 2: Update status and triggeredAt
    device.status = status;
    device.signalStrength = signalStrength;
    device.triggeredAt = new Date();
    await device.save();

    // Optional: log or update user.alerts if still needed
    const user = await User.findById(userId);
    if (user) {
      if (!user.alerts) user.alerts = [];
      user.alerts.push({
        triggeredAt: new Date(),
        type: `connectivity-${status}`,
      });
      await user.save();
    }

    res.status(200).json({ message: "Device connectivity status updated", device });
  } catch (err) {
    console.error("Connectivity update error:", err.stack);
    res.status(500).json({ message: "Server error" });
  }
};