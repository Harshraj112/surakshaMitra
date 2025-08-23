import EmergencyLog from "../model/emergencyLog.js";

export const logEmergency = async (req, res) => {
  const { lat, lng, triggerType, voiceClipUrl } = req.body;
  const userId = req.userId; // comes from auth middleware

  if (!lat || !lng || !triggerType) {
    return res.status(400).json({ message: "Location and triggerType required" });
  }

  try {
    const log = new EmergencyLog({
      userId,
      location: { lat, lng },
      triggerType,
      voiceClipUrl
    });

    await log.save();
    res.status(201).json({ message: "Emergency logged successfully", log });
  } catch (err) {
    console.error("Error saving emergency log:", err);
    res.status(500).json({ message: "Server error" });
  }
};

// module.exports = { logEmergency };