// routes/jobs.js
// Core marketplace endpoints: households post jobs, workers browse and
// accept them. This is the data the Android app's job feed / job detail
// / post-job screens talk to.
const express = require("express");
const Job = require("../models/Job");
const User = require("../models/User");
const { requireAuth } = require("../middleware/auth");
const { notifyMatchingWorkers, notifyJobAccepted, notifyJobCompleted } = require("../utils/notify");

const router = express.Router();

// GET /api/jobs  — job feed (optionally filter by status/category)
router.get("/", requireAuth, async (req, res) => {
  try {
    const { status, category } = req.query;
    const where = {};
    if (status) where.status = status;
    if (category) where.category = category;

    const jobs = await Job.findAll({
      where,
      order: [["createdAt", "DESC"]],
      include: [
        { model: User, as: "postedBy", attributes: ["id", "name"] },
        { model: User, as: "acceptedBy", attributes: ["id", "name"] },
      ],
    });

    return res.json(jobs);
  } catch (err) {
    console.error("[jobs] List error:", err.message);
    return res.status(500).json({ error: "Server error fetching jobs" });
  }
});

// GET /api/jobs/:id
router.get("/:id", requireAuth, async (req, res) => {
  try {
    const job = await Job.findByPk(req.params.id, {
      include: [
        { model: User, as: "postedBy", attributes: ["id", "name"] },
        { model: User, as: "acceptedBy", attributes: ["id", "name"] },
      ],
    });
    if (!job) return res.status(404).json({ error: "Job not found" });
    return res.json(job);
  } catch (err) {
    console.error("[jobs] Get error:", err.message);
    return res.status(500).json({ error: "Server error fetching job" });
  }
});

// POST /api/jobs  — household posts a new job
router.post("/", requireAuth, async (req, res) => {
  try {
    const { title, description, category, location, budget } = req.body;

    if (!title || !description || !category || !location || budget == null) {
      return res.status(400).json({ error: "All job fields are required" });
    }
    if (isNaN(Number(budget)) || Number(budget) < 0) {
      return res.status(400).json({ error: "Budget must be a valid positive number" });
    }

    const job = await Job.create({
      title,
      description,
      category,
      location,
      budget: Number(budget),
      postedById: req.user.id,
    });

    console.log(`[jobs] New job posted: "${job.title}" by user ${req.user.id}`);

    // Feature: Targeted job alerts — notify only workers whose skills
    // match this job's category, not every worker on the platform.
    notifyMatchingWorkers(job).catch((e) =>
      console.error("[jobs] Failed to send job-match notifications:", e.message)
    );

    return res.status(201).json(job);
  } catch (err) {
    console.error("[jobs] Create error:", err.message);
    return res.status(500).json({ error: "Server error creating job" });
  }
});

// PUT /api/jobs/:id/accept  — worker accepts an open job
router.put("/:id/accept", requireAuth, async (req, res) => {
  try {
    const job = await Job.findByPk(req.params.id);
    if (!job) return res.status(404).json({ error: "Job not found" });
    if (job.status !== "open") {
      return res.status(409).json({ error: "This job is no longer available" });
    }

    job.status = "accepted";
    job.acceptedById = req.user.id;
    await job.save();

    console.log(`[jobs] Job ${job.id} accepted by user ${req.user.id}`);

    const worker = await User.findByPk(req.user.id);
    notifyJobAccepted(job, worker ? worker.name : "A worker").catch((e) =>
      console.error("[jobs] Failed to send accept notification:", e.message)
    );

    return res.json({ message: "Job accepted", job });
  } catch (err) {
    console.error("[jobs] Accept error:", err.message);
    return res.status(500).json({ error: "Server error accepting job" });
  }
});

// PUT /api/jobs/:id/complete — mark an accepted job as completed
router.put("/:id/complete", requireAuth, async (req, res) => {
  try {
    const job = await Job.findByPk(req.params.id);
    if (!job) return res.status(404).json({ error: "Job not found" });
    if (job.status !== "accepted") {
      return res.status(409).json({ error: "Only accepted jobs can be completed" });
    }
    const isParticipant = req.user.id === job.postedById || req.user.id === job.acceptedById;
    if (!isParticipant) {
      return res.status(403).json({ error: "Only the household or worker on this job can complete it" });
    }

    job.status = "completed";
    await job.save();

    console.log(`[jobs] Job ${job.id} marked complete by user ${req.user.id}`);

    notifyJobCompleted(job).catch((e) =>
      console.error("[jobs] Failed to send completion notifications:", e.message)
    );

    return res.json({ message: "Job marked complete", job });
  } catch (err) {
    console.error("[jobs] Complete error:", err.message);
    return res.status(500).json({ error: "Server error completing job" });
  }
});

module.exports = router;
