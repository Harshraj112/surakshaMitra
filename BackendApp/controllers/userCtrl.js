import User from "../model/user.js";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import { getTokenFromHeader } from "../utils/getTokenFromHeader.js";
import generateToken from "../utils/generateToken.js";
import asyncHandler from "express-async-handler";
import otpGenerator from 'otp-generator';
import nodemailer from 'nodemailer';
import dotenv from "dotenv";
dotenv.config();
import twilio from "twilio"; // or Fast2SMS alternative



//////////////////OTP Functionality///////////////

// Nodemailer transporter
// console.log("Email user:", process.env.EMAIL_USER);
// console.log("Email pass:", process.env.EMAIL_PASS);

const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: process.env.EMAIL_USER,
    pass: process.env.EMAIL_PASS,
  },
});



// ==============================
// @desc    Send OTP to user email
// @route   POST /api/v1/users/send-otp
// @access  Public
// ==============================
// temporary in-memory store (in real apps, use Redis or similar)
// In-memory store (use Redis or DB in production)
let pendingOtps = {};       // key: email/phone, value: { otp, expires }
let pendingUsers = {};      // key: email/phone, value: { form data, timestamp }


export const sendOtpCtrl = asyncHandler(async (req, res) => {
  let { fullname, email, phone, password, emergencyContacts, locationHistory } = req.body;
  email = email?.toLowerCase();

  if (!fullname || !email || !phone || !password) {
    res.status(400);
    throw new Error("fullname, email, phone, and password are required to send OTP");
  }

  const existingUser = await User.findOne({ $or: [{ email }, { phone }] });
  if (existingUser) {
    res.status(400);
    throw new Error("User with this email or phone already exists");
  }

  const otp = Math.floor(100000 + Math.random() * 900000).toString();
  const hashedOtp = await bcrypt.hash(otp, 10);
  const key = email || phone;

  // Format emergency contacts
const formatEmergencyContacts = (contacts) => {
  if (!Array.isArray(contacts)) return [];
  return contacts.map((contact, index) => {
    if (typeof contact === 'string') {
      return {
        name: `Emergency Contact ${index + 1}`,
        phone: contact.replace(/\D/g, '')
      };
    }
    if (typeof contact === 'object' && contact !== null) {
      return {
        name: contact.name || `Emergency Contact ${index + 1}`,
        phone: contact.phone ? contact.phone.replace(/\D/g, '') : ''
      };
    }
    return null;
  }).filter(Boolean);
};

const formattedContacts = formatEmergencyContacts(emergencyContacts);


  // Store OTP
  pendingOtps[key] = {
    otp: hashedOtp,
    otpExpires: Date.now() + 3 * 60 * 1000 // 3 minute
  };

  // Store form data
  pendingUsers[key] = {
    fullname,
    email,
    phone,
    password,
    emergencyContacts: formattedContacts,
    locationHistory: locationHistory || [],
    createdAt: Date.now()
  };

  try {
    await transporter.sendMail({
      from: process.env.EMAIL_USER,
      to: email,
      subject: "🔐 Your OTP Verification Code",
      html: `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>OTP Verification</title>
    </head>
    <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f7fa; line-height: 1.6;">
      <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
        
        <!-- Header -->
        <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px 20px; text-align: center;">
          <div style="background-color: #ffffff; width: 60px; height: 60px; border-radius: 50%; margin: 0 auto 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); text-align: center; line-height: 60px;">
            <div style="font-size: 24px; line-height: 60px; vertical-align: middle;">🔐</div>
          </div>
          <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 300;">Security Verification</h1>
        </div>
        
        <!-- Content -->
        <div style="padding: 40px 30px;">
          <div style="text-align: center; margin-bottom: 30px;">
            <h2 style="color: #333333; margin: 0 0 15px 0; font-size: 24px; font-weight: 500;">Your Verification Code</h2>
            <p style="color: #666666; margin: 0; font-size: 16px;">Please use the following code to complete your verification:</p>
          </div>
          
          <!-- OTP Box -->
          <div style="text-align: center; margin: 30px 0;">
            <div style="display: inline-block; background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); padding: 20px 40px; border-radius: 12px; box-shadow: 0 4px 15px rgba(240, 147, 251, 0.3);">
              <div style="color: #ffffff; font-size: 36px; font-weight: bold; letter-spacing: 8px; font-family: 'Courier New', monospace;">${otp}</div>
            </div>
          </div>
          
          <!-- Timer -->
          <div style="text-align: center; margin: 25px 0;">
            <div style="display: inline-flex; align-items: center; background-color: #fff3cd; color: #856404; padding: 12px 20px; border-radius: 8px; border-left: 4px solid #ffc107;">
              <span style="margin-right: 8px;">⏰</span>
              <span style="font-weight: 500;">This code expires in 3 minute</span>
            </div>
          </div>
          
          <!-- Security Notice -->
          <div style="background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 25px 0; border-left: 4px solid #17a2b8;">
            <div style="display: flex; align-items: flex-start;">
              <span style="margin-right: 10px; font-size: 18px;">🛡️</span>
              <div>
                <h3 style="color: #495057; margin: 0 0 8px 0; font-size: 16px; font-weight: 600;">Security Notice</h3>
                <p style="color: #6c757d; margin: 0; font-size: 14px;">
                  If you didn't request this code, please ignore this email. Never share your OTP with anyone.
                </p>
              </div>
            </div>
          </div>
          
          <!-- Trouble Section -->
          <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #e9ecef;">
            <p style="color: #6c757d; margin: 0; font-size: 14px;">
              Having trouble? Contact our support team at 
              <a href="mailto:surakshamitra112@gmail.com" style="color: #667eea; text-decoration: none;">surakshamitra112.netlify.app</a>
            </p>
          </div>
        </div>
        
        <!-- Footer -->
        <div style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-top: 1px solid #e9ecef;">
          <p style="color: #6c757d; margin: 0; font-size: 12px;">
            © 2025 surakshaMitra. All rights reserved.<br>
            This is an automated message, please do not reply to this email.
          </p>
        </div>
        
      </div>
    </body>
    </html>
    `
    });

    res.status(200).json({
      status: "success",
      message: `OTP sent to ${email}`
    });
  } catch (error) {
    delete pendingOtps[key];
    delete pendingUsers[key];
    res.status(500);
    throw new Error("Failed to send OTP");
  }
});





// ==============================
// @desc    Verify user with OTP
// @route   POST /api/v1/users/verify-otp
// @access  Public
// ==============================
export const verifyOtpCtrl = asyncHandler(async (req, res) => {
  let { email, otp } = req.body;
  email = email?.toLowerCase();

  if (!email || !otp) {
    res.status(400);
    throw new Error("Email and OTP are required");
  }

  const key = email;
  const pendingOtp = pendingOtps[key];
  const userData = pendingUsers[key];

  if (!pendingOtp || !userData) {
    res.status(400);
    throw new Error("OTP expired or registration not initiated");
  }

  const isOtpValid = await bcrypt.compare(otp.toString(), pendingOtp.otp);
  const isExpired = Date.now() > pendingOtp.otpExpires;

  if (isExpired) {
    delete pendingOtps[key];
    delete pendingUsers[key];
    res.status(400);
    throw new Error("OTP expired. Please request a new one.");
  }

  if (!isOtpValid) {
    res.status(400);
    throw new Error("Invalid OTP. Please try again.");
  }

  // ✅ Convert emergencyContacts (objects → phone numbers) if needed
  let emergencyNumbers = [];
  if (
    Array.isArray(userData.emergencyContacts) &&
    userData.emergencyContacts.length > 0
  ) {
    emergencyNumbers = userData.emergencyContacts.map((c) => {
      if (typeof c === "string") return c;
      return c.phone;
    });
  }

  // Create user
  const hashedPassword = await bcrypt.hash(userData.password, 10);
  const newUser = await User.create({
    fullname: userData.fullname,
    email: userData.email,
    phone: userData.phone,
    password: hashedPassword,
    isVerified: true,
    emergencyContacts: emergencyNumbers || [],
    locationHistory: userData.locationHistory || [],
    alerts: []
  });

  // Clean up
  delete pendingOtps[key];
  delete pendingUsers[key];

  res.status(201).json({
    status: "success",
    message: "User registered successfully",
    // user: {
    //   // _id: newUser._id,
    //   fullname: newUser.fullname,
    //   email: newUser.email,
    //   phone: newUser.phone,
    //   emergencyContacts: newUser.emergencyContacts,
    //   createdAt: newUser.createdAt,
    //   // Add more fields here if needed (e.g., emergencyContacts)
    // },
    userId: newUser._id,
    token: generateToken(newUser._id),
  });
});




// ==============================
// @desc    Resend OTP
// @route   POST /api/v1/users/resend-otp
// @access  Public
// ==============================
export const resendOtpCtrl = asyncHandler(async (req, res) => {
  let { email } = req.body;
  const key = email?.toLowerCase();

  if (!email) {
    res.status(400);
    throw new Error("Email is required.");
  }

  const userData = pendingUsers[email];
  if (!userData) {
    res.status(400);
    throw new Error("No pending registration found for this email.");
  }

  // Generate and hash new OTP
  const otp = Math.floor(100000 + Math.random() * 900000).toString();
  const hashedOtp = await bcrypt.hash(otp, 10);

  // Store hashed OTP and expiration time
  pendingOtps[email] = {
    otp: hashedOtp,
    otpExpires: Date.now() + 10 * 60 * 1000, // 10 minutes
  };

  // Compose email HTML content
  const mailOptions = {
    from: process.env.EMAIL_USER,
    to: email,
    subject: "🔐 Your OTP Verification Code(Resent)",
    html: `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>OTP Verification</title>
    </head>
    <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f7fa; line-height: 1.6;">
      <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
        
        <!-- Header -->
        <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px 20px; text-align: center;">
          <div style="background-color: #ffffff; width: 60px; height: 60px; border-radius: 50%; margin: 0 auto 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); text-align: center; line-height: 60px;">
            <div style="font-size: 24px; line-height: 60px; vertical-align: middle;">🔐</div>
          </div>
          <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 300;">Security Verification</h1>
        </div>
        
        <!-- Content -->
        <div style="padding: 40px 30px;">
          <div style="text-align: center; margin-bottom: 30px;">
            <h2 style="color: #333333; margin: 0 0 15px 0; font-size: 24px; font-weight: 500;">Your Verification Code</h2>
            <p style="color: #666666; margin: 0; font-size: 16px;">Please use the following code to complete your verification:</p>
          </div>
          
          <!-- OTP Box -->
          <div style="text-align: center; margin: 30px 0;">
            <div style="display: inline-block; background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); padding: 20px 40px; border-radius: 12px; box-shadow: 0 4px 15px rgba(240, 147, 251, 0.3);">
              <div style="color: #ffffff; font-size: 36px; font-weight: bold; letter-spacing: 8px; font-family: 'Courier New', monospace;">${otp}</div>
            </div>
          </div>
          
          <!-- Timer -->
          <div style="text-align: center; margin: 25px 0;">
            <div style="display: inline-flex; align-items: center; background-color: #fff3cd; color: #856404; padding: 12px 20px; border-radius: 8px; border-left: 4px solid #ffc107;">
              <span style="margin-right: 8px;">⏰</span>
              <span style="font-weight: 500;">This code expires in 3 minute</span>
            </div>
          </div>
          
          <!-- Security Notice -->
          <div style="background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 25px 0; border-left: 4px solid #17a2b8;">
            <div style="display: flex; align-items: flex-start;">
              <span style="margin-right: 10px; font-size: 18px;">🛡️</span>
              <div>
                <h3 style="color: #495057; margin: 0 0 8px 0; font-size: 16px; font-weight: 600;">Security Notice</h3>
                <p style="color: #6c757d; margin: 0; font-size: 14px;">
                  If you didn't request this code, please ignore this email. Never share your OTP with anyone.
                </p>
              </div>
            </div>
          </div>
          
          <!-- Trouble Section -->
          <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #e9ecef;">
            <p style="color: #6c757d; margin: 0; font-size: 14px;">
              Having trouble? Contact our support team at 
              <a href="mailto:surakshamitra112@gmail.com" style="color: #667eea; text-decoration: none;">surakshamitra112.netlify.app</a>
            </p>
          </div>
        </div>
        
        <!-- Footer -->
        <div style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-top: 1px solid #e9ecef;">
          <p style="color: #6c757d; margin: 0; font-size: 12px;">
            © 2025 surakshaMitra. All rights reserved.<br>
            This is an automated message, please do not reply to this email.
          </p>
        </div>
        
      </div>
    </body>
    </html>
    `
  };

  try {
    await transporter.sendMail(mailOptions);
    res.status(200).json({
      status: "success",
      message: `OTP resent to ${email}`,
    });
  } catch (error) {
    console.error("Error sending OTP email:", error);
    res.status(500);
    throw new Error("Failed to resend OTP. Please try again later.");
  }
});


///////////////////////////////////////////////////////////////



/**************************Not required**********************/
///////////////////REGISTRER USERS////////////////////////////

// ==============================
// @desc    Register user
// @route   POST /api/v1/users/register
// @access  Public
// ==============================
// export const registerUser = asyncHandler(async (req, res) => {
//   const { fullname, email, password, phone } = req.body;

//   // Check if email already exists
//   const existingEmail = await User.findOne({ email });
//   if (existingEmail) {
//     res.status(400);
//     throw new Error("Email already in use");
//   }

//   // Check if phone already exists
//   const existingPhone = await User.findOne({ phone });
//   if (existingPhone) {
//     res.status(400);
//     throw new Error("Phone number already in use");
//   }

//   // Hash password
//   const salt = await bcrypt.genSalt(10);
//   const hashedPassword = await bcrypt.hash(password, salt);

//   // Create new user
//   const user = await User.create({
//     fullname,
//     email,
//     password: hashedPassword,
//     phone,
//   });

//   if (user) {
//     res.status(201).json({
//         status: "success",
//         message: "User Registered Successfully",
//         data: user,
//         token: generateToken(user._id),
//     });
//   } else {
//     res.status(400);
//     throw new Error("Invalid user data");
//   }
// });




// ==============================
// @desc    Login user
// @route   POST /api/v1/users/login
// @access  Public
// ==============================
export const loginUser = asyncHandler(async (req, res) => {
  const { email, password } = req.body;

  const user = await User.findOne({ email });
  if (!user || !(await bcrypt.compare(password, user.password))) {
    res.status(401);
    throw new Error("Invalid email or password");
  }

  if (!user.isVerified) {
    res.status(403);
    throw new Error("Please verify your account via OTP before logging in");
  }

  res.status(200).json({
    status: "success",
    message: "User logged in successfully",
    user: {
      fullname: user.fullname,
      email: user.email,
      phone: user.phone,
      emergencyContacts: user.emergencyContacts, // ✅ this will now be an array of strings
      createdAt: user.createdAt,
    },
    userId: user._id,
    token: generateToken(user._id),
  });
});




// @desc    Get authenticated user profile
// @route   GET /api/users/profile
// @access  Private (handled in middleware)
export const getUserProfileCtrl = asyncHandler(async (req, res) => {
  const userId = req.user._id; // ✅ this works
  // console.log("User ID from req.userAuthId:", req.userAuthId);


  const user = await User.findById(userId).select(
    "-password -otp -otpExpires"
  );

  if (!user) {
    res.status(404);
    throw new Error("User not found");
  }

  res.status(200).json({
    status: "success",
    message: "User profile fetched successfully",
    user: {
      id: user._id,
      fullname: user.fullname,
      email: user.email,
      phone: user.phone,
      emergencyContacts: user.emergencyContacts,
      isVerified: user.isVerified,
      createdAt: user.createdAt,
      alertsCount: user.alerts?.length || 0,
      locationHistoryCount: user.locationHistory?.length || 0,
    },
  });
});





// ==============================
// @desc    Update user contact info
// @route   PUT /api/v1/users/update/contact
// @access  Private
// ==============================
export const updateContactCtrl = asyncHandler(async (req, res) => {
  const user = await User.findById(req.userAuthId);
  if (!user) {
    res.status(404);
    throw new Error("User not found");
  }

  // Fields that should NOT be updated
  const protectedFields = ["email", "phone"];

  // Update allowed fields (excluding protected ones and handling password separately)
  Object.keys(req.body).forEach((key) => {
    if (!protectedFields.includes(key) && key !== "password") {
      user[key] = req.body[key];
    }
  });

  // Handle password hashing
  if (req.body.password) {
    const hashedPassword = await bcrypt.hash(req.body.password, 10);
    user.password = hashedPassword;
  }

  await user.save();

  res.status(200).json({
    status: "success",
    message: "User info updated",
    user,
  });
});




// ==============================
// @desc    Delete user
// @route   DELETE /api/v1/users/delete
// @access  Private/Admin
// ==============================
export const deleteUserCtrl = asyncHandler(async (req, res) => {
  const user = await User.findByIdAndDelete(req.userAuthId);

  if (!user) {
    res.status(404);
    throw new Error("User not found");
  }

  res.status(200).json({
    status: "success",
    message: `User ${user.email} deleted successfully`,
  });
});



// ==============================
// @desc    Update an emergency contact
// @route   PUT /api/v1/users/emergency-contacts/:contactId
// @access  Private/User
// ==============================
export const updateEmergencyContactCtrl = asyncHandler(async (req, res) => {
  const user = req.user;
  const { emergencyContacts } = req.body;

  if (!Array.isArray(emergencyContacts)) {
    res.status(400);
    throw new Error("emergencyContacts must be an array of 10-digit phone numbers");
  }

  const valid = emergencyContacts.every((num) => /^[0-9]{10}$/.test(String(num)));
  if (!valid) {
    res.status(400);
    throw new Error("Each phone number must be a 10-digit number");
  }

  const uniqueContacts = [...new Set(emergencyContacts)];

  // Option 1: Save only phone numbers (strings)
  user.emergencyContacts = uniqueContacts;
  
  // Option 2: If you update schema to accept objects
  // user.emergencyContacts = uniqueContacts.map((phone) => ({
  //   phone: String(phone),
  //   name: "Emergency",
  // }));

  await user.save();

  res.status(200).json({
    status: "success",
    message: "Emergency contacts updated",
  });
});


////////////////////////////////////////////////////////////////





////////////// SMS Alert Functionality //////////////

const accountSid = process.env.TWILIO_ACCOUNT_SID;
const authToken = process.env.TWILIO_AUTH_TOKEN;
const client = twilio(accountSid, authToken);

export const sendSMS = async (to, msg) => {
  await client.messages.create({
    body: msg,
    from: +12722133365,
    to: `+91${to}`, // ensure correct format
  });

  // await client.messages.create({
  //   body: msg,
  //   from: 'whatsapp:+12722133365',
  //   to: `whatsapp:+91${to}`,
  //   // body: message
  // });
  console.log(`SMS sent to ${to}: ${msg}`);
};


// ==============================
// @desc    Send SMS alert
// @route   POST /api/v1/users/sms-alert
// @access  Private
// ==============================
export const triggerSOS = asyncHandler(async (req, res) => {
  const { lat, lng } = req.body;

  const user = await User.findById(req.userAuthId);
  if (!user || user.emergencyContacts.length === 0) {
    res.status(404);
    throw new Error("User or emergency contacts not found");
  }

  const message = `🚨 SafePulse Alert 🚨\n${user.fullname} may be in danger!\nLocation: https://www.google.com/maps?q=${lat},${lng}`;

  // Loop through all contacts
  for (let contact of user.emergencyContacts) {
  try {
    await sendSMS(contact.phone, message);
  } catch (err) {
    console.error(`Failed to send SMS to ${contact.phone}:`, err.message);
  }
}
  res.status(200).json({ status: "success", message: "SOS alerts sent" });
});

//////////////////////////////////////////////////////
