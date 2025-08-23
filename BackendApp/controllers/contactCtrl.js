// controllers/contactController.js
import asyncHandler from "express-async-handler";
import User from "../model/user.js";

export const deleteEmergencyContact = async (req, res) => {
  const contactId = req.params.id;

  try {
    // Find the user making the request
    console.log(req.userAuthId);
    const user = await User.findById(req.userAuthId);

    if (!user) {
      return res.status(404).json({ message: "User not found" });
    }

    // Filter out the emergency contact by id
    const originalLength = user.emergencyContacts.length;

    user.emergencyContacts = user.emergencyContacts.filter(
      (contact) => contact._id.toString() !== contactId
    );

    if (user.emergencyContacts.length === originalLength) {
      return res.status(404).json({ message: "Emergency contact not found" });
    }

    // Save the updated user
    await user.save();

    res.status(200).json({ message: "Emergency contact deleted successfully" });
  } catch (error) {
    console.error("Delete contact error:", error);
    res.status(500).json({ message: "Server error" });
  }
};