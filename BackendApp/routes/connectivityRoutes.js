import express from 'express';
import { updateConnectivityStatus } from '../controllers/deviceCtrl.js';

const router = express.Router();
router.post("/connectivity-status", updateConnectivityStatus);

export default router;