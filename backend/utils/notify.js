// utils/notify.js
// Small helper used by routes/jobs.js and routes/ratings.js to create
// targeted in-app alerts without duplicating the same Notification.create
// call in five different places.
const Notification = require("../models/Notification");
const User = require("../models/User");

/**
 * Notifies every worker whose comma-separated `skills` field contains the
 * job's category. This is the "targeted" part of "targeted job alerts" —
 * it does not notify every worker, only ones with a matching skill.
 * A worker with no skills set is treated as open to every category
 * (useful for brand-new accounts that haven't filled in a profile yet).
 */
async function notifyMatchingWorkers(job) {
  const workers = await User.findAll({ where: { role: "worker" } });

  const matches = workers.filter((w) => {
    if (!w.skills) return true; // no skills set yet -> see everything
    const skillList = w.skills.split(",").map((s) => s.trim().toLowerCase());
    return skillList.includes(job.category.toLowerCase());
  });

  await Promise.all(
    matches.map((worker) =>
      Notification.create({
        userId: worker.id,
        type: "JOB_MATCH",
        jobId: job.id,
        message: `New ${job.category} job near ${job.location}: "${job.title}" (R${job.budget})`,
      })
    )
  );

  console.log(`[notify] Job ${job.id} matched to ${matches.length} worker(s)`);
}

async function notifyJobAccepted(job, workerName) {
  await Notification.create({
    userId: job.postedById,
    type: "JOB_ACCEPTED",
    jobId: job.id,
    message: `${workerName} accepted your job "${job.title}"`,
  });
}

async function notifyJobCompleted(job) {
  // Let both parties know a rating is now possible.
  await Notification.create({
    userId: job.postedById,
    type: "JOB_COMPLETED",
    jobId: job.id,
    message: `"${job.title}" is marked complete. You can now rate the worker.`,
  });
  if (job.acceptedById) {
    await Notification.create({
      userId: job.acceptedById,
      type: "JOB_COMPLETED",
      jobId: job.id,
      message: `"${job.title}" is marked complete. You can now rate the household.`,
    });
  }
}

async function notifyRatingReceived(rateeId, score, jobTitle) {
  await Notification.create({
    userId: rateeId,
    type: "RATING_RECEIVED",
    message: `You received a ${score}-star rating for "${jobTitle}"`,
  });
}

module.exports = {
  notifyMatchingWorkers,
  notifyJobAccepted,
  notifyJobCompleted,
  notifyRatingReceived,
};
