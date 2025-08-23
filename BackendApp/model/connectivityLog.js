// models/connectivityLog.js
import mongoose from "mongoose";
const Schema = mongoose.Schema;

const connectivityLogSchema = new Schema({
  user: {
    type: Schema.Types.ObjectId,
    ref: "User",
    required: true
  },
  
});

const ConnectivityLog = mongoose.model("ConnectivityLog", connectivityLogSchema);

export default ConnectivityLog;
