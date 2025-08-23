import express from "express";
const router = express.Router();

import { isLoggedIn } from "../middleware/isLoggedIn.js";
import { logEmergency } from "../controllers/emergencyCtrl.js"
// const verifyToken = require("../middleware/isLoggedIn.js");

router.post("/emergency-log", isLoggedIn, logEmergency);

export default router;