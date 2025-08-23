import dotenv from "dotenv";
dotenv.config();
import express from "express";
import dbConnect from "../config/dbConnect.js";
import userRoutes from "../routes/userRoute.js";
import deviceRoute from "../routes/deviceRoute.js"
import EmergencyRoute from "../routes/emergencyRoutes.js"
import connectivityRoutes from "../routes/connectivityRoutes.js";
import contactRoutes from "../routes/contactRoutes.js"
import morgan from 'morgan';
import cors from "cors";
import { globalErrHandler } from "../middleware/globalErrHandler.js";

const app = express();
dbConnect();
app.use(cors());

// Use morgan middleware
app.use(morgan('dev')); // 'dev' is a predefined format string


app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// HEAD route to stop 404 for HEAD /
app.head('/', (req, res) => {
  res.sendStatus(200);
});


app.use("/api/v1/users", userRoutes);
app.use("/api/v1/device", deviceRoute);
app.use("/api/v1/emergencylog", EmergencyRoute);
// app.use("/api/v1/device", deviceRoute);
app.use("/api/v1", connectivityRoutes);
app.use("/api/v1", contactRoutes);

app.get("/api/health", (req, res) => {
  res.status(200).send("OK");
});

app.use((req, res, next) => {
  res.status(404).type('text').send('404 Not Found - Invalid URL');
});

// // 🧹 Auto-clean expired OTPs every 60 seconds
// setInterval(() => {
//   const now = Date.now();
//   for (let key in pendingOtps) {
//     if (pendingOtps[key].otpExpires < now) {
//       console.log(`OTP expired for: ${key}`);
//       delete pendingOtps[key];
//       delete pendingUsers[key];
//     }
//   }
// }, 60 * 1000); // Runs every 1 minute
app.use(globalErrHandler);

export default app;
