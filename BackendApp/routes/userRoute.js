import express from "express";
import { loginUser, getUserProfileCtrl, updateContactCtrl, deleteUserCtrl, sendOtpCtrl, verifyOtpCtrl,resendOtpCtrl, triggerSOS, updateEmergencyContactCtrl } from "../controllers/userCtrl.js";
import { isLoggedIn } from "../middleware/isLoggedIn.js";


const router = express.Router();

// Route to register a new user
// router.post("/register", registerUser);

// Route to login a user
router.post("/login", loginUser);

// Route to get user profile (protected route)
router.get("/profile/:id",isLoggedIn, getUserProfileCtrl);

// Route to update user contact information (protected route)
router.put("/update/contact",isLoggedIn, updateContactCtrl);

//Route to delete user account (protected route)
router.delete("/delete",isLoggedIn, deleteUserCtrl);

// @route   POST /api/users/register
router.post('/send-otp', sendOtpCtrl);

// @route   POST /api/users/verify-otp
router.post('/verify-otp', verifyOtpCtrl);

// @route
router.post('/resend-otp', resendOtpCtrl);

// @route   POST /api/users/trigger-sos
router.post('/sms-alert', isLoggedIn, triggerSOS);

// @route   POST /api/users/update-emergency-contact
router.put('/update/emergency-contacts', isLoggedIn, updateEmergencyContactCtrl);

// Export the router
export default router;