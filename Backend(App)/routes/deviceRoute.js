import express from "express";
import { getEmergencyContacts } from "../controllers/deviceCtrl.js";
const router = express.Router();

import { syncDevice } from "../controllers/deviceCtrl.js";

router.post("/device-sync", syncDevice);
router.get("/:deviceId/emergency-contacts", getEmergencyContacts);

export default router;
