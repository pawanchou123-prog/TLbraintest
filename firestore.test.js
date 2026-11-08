const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_1_UID = "admin_pritam_uid";
const ADMIN_1_EMAIL = "choudharyriti92@gmail.com";
const ADMIN_2_UID = "admin_pawan_uid";
const ADMIN_2_EMAIL_MIXED = "Pawanchou123@gmail.com";
const UNAUTH_GOOGLE_UID = "intruder_uid";
const UNAUTH_GOOGLE_EMAIL = "randomperson@gmail.com";
const ALICE_UID = "student_alice";
const ALICE_EMAIL = "alice@student.com";
const BOB_UID = "student_bob";
const BOB_EMAIL = "bob@student.com";
const QM_UID = "qm_operator";
const QM_EMAIL = "teacher.sharma@gmail.com";
const QM_EMAIL_KEY = "teacher_sharma_gmail_com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: rejected across student, admin, and question-manager collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("questions").where("isPublished", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("allowed_students").get());
  await assertFails(unauthDb.collection("allowed_question_managers").get());
  await assertFails(unauthDb.collection("question_audit_logs").get());
  await assertFails(unauthDb.collection("subscription_records").get());
  await assertFails(unauthDb.collection("admin_audit_logs").get());
});

test("Email Spoofing & Self-Assigned Admin/Manager Role Guard: unverified emails and self-assigned roles rejected", async () => {
  const spoofAdmin1 = testEnv.authenticatedContext("spoof_1", {
    email: ADMIN_1_EMAIL,
    email_verified: false,
  }).firestore();
  const spoofAdmin2 = testEnv.authenticatedContext("spoof_2", {
    email: ADMIN_2_EMAIL_MIXED,
    email_verified: false,
  }).firestore();
  const intruderDb = testEnv.authenticatedContext(UNAUTH_GOOGLE_UID, {
    email: UNAUTH_GOOGLE_EMAIL,
    email_verified: true,
  }).firestore();

  await assertFails(spoofAdmin1.collection("allowed_students").get());
  await assertFails(spoofAdmin2.collection("allowed_students").get());
  await assertFails(intruderDb.collection("allowed_students").get());
  await assertFails(intruderDb.collection("allowed_question_managers").get());

  const now = new Date();
  await assertFails(
    intruderDb.collection("admins").doc(UNAUTH_GOOGLE_UID).set({
      uid: UNAUTH_GOOGLE_UID,
      email: UNAUTH_GOOGLE_EMAIL,
      name: "Intruder",
      role: "admin",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Intruder cannot self-assign Question Manager role without Main Admin allowlist
  await assertFails(
    intruderDb.collection("question_managers").doc(UNAUTH_GOOGLE_UID).set({
      uid: UNAUTH_GOOGLE_UID,
      email: UNAUTH_GOOGLE_EMAIL,
      name: "Intruder",
      role: "question_manager",
      emailKey: "randomperson_gmail_com",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Both Authorized Main Admins (Admin 1 & Admin 2) have full Main Admin permissions", async () => {
  const admin1Db = testEnv.authenticatedContext(ADMIN_1_UID, {
    email: ADMIN_1_EMAIL,
    email_verified: true,
  }).firestore();
  const admin2Db = testEnv.authenticatedContext(ADMIN_2_UID, {
    email: ADMIN_2_EMAIL_MIXED,
    email_verified: true,
  }).firestore();
  const now = new Date();

  await assertSucceeds(
    admin1Db.collection("allowed_students").doc("alice_student_com").set({
      email: ALICE_EMAIL,
      studentName: "Alice Sharma",
      studentClass: 10,
      preferredLanguage: "en",
      isBanned: false,
      subscriptionActive: true,
      subscriptionPlan: "Rs. 50 / 3 Months",
      paymentMethod: "upi",
      subscriptionStartMs: 1700000000000,
      subscriptionEndMs: 1799000000000,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(admin2Db.collection("allowed_students").get());
  await assertSucceeds(
    admin2Db.collection("app_config").doc("global").set({
      leaderboardEnabled: true,
      currentMonthKey: "2026-11",
      youtubeUrl: "https://www.youtube.com/@tlbraintest",
      whatsappUrl: "https://wa.me/910000000000",
      upiId: "pritam@upi",
      subscriptionPriceText: "Rs. 50 for 3 Months",
      supportContactText: "Contact Admin",
      appVersionBanner: "TL Brain Test v1.0",
      termsAndConditionsEn: "Official Terms.",
      monthlyResetDay: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    admin2Db.collection("admin_audit_logs").doc("audit_1").set({
      adminUid: ADMIN_2_UID,
      adminEmail: ADMIN_2_EMAIL_MIXED,
      actionType: "update_config",
      targetId: "global",
      targetLabel: "Global AppConfig",
      reasonOrDetails: "Updated YouTube and UPI details",
      pointsDelta: 0,
      createdAt: now,
    })
  );

  await assertFails(admin1Db.collection("admin_audit_logs").doc("audit_1").delete());
});

test("Student Account Denial on Admin Resources & Safe Manual Subscription Request Flow", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: ALICE_EMAIL,
    email_verified: true,
  }).firestore();
  const admin1Db = testEnv.authenticatedContext(ADMIN_1_UID, {
    email: ADMIN_1_EMAIL,
    email_verified: true,
  }).firestore();
  const now = new Date();

  await assertFails(aliceDb.collection("allowed_students").get());
  await assertFails(aliceDb.collection("users").get());
  await assertFails(aliceDb.collection("support_threads").get());
  await assertFails(aliceDb.collection("admin_audit_logs").get());
  await assertFails(aliceDb.collection("subscription_records").get());

  await assertFails(
    aliceDb.collection("subscription_records").doc("req_hack").set({
      studentEmail: ALICE_EMAIL,
      studentName: "Alice",
      studentClass: 10,
      studentUid: ALICE_UID,
      amountInr: 50,
      planLabel: "Rs. 50 / 3 Months",
      paymentMethod: "upi",
      referenceCode: "UTR999",
      status: "approved",
      startMs: 1700000000000,
      endMs: 1799000000000,
      verificationNotes: "Self approved",
      verifiedByAdminEmail: ALICE_EMAIL,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("subscription_records").doc("req_valid").set({
      studentEmail: ALICE_EMAIL,
      studentName: "Alice",
      studentClass: 10,
      studentUid: ALICE_UID,
      amountInr: 50,
      planLabel: "Rs. 50 / 3 Months",
      paymentMethod: "upi",
      referenceCode: "UTR12345678",
      status: "pending",
      startMs: 0,
      endMs: 0,
      verificationNotes: "",
      verifiedByAdminEmail: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(admin1Db.collection("subscription_records").get());
  await assertSucceeds(
    admin1Db.collection("subscription_records").doc("req_valid").update({
      status: "approved",
      startMs: 1700000000000,
      endMs: 1707776000000,
      verificationNotes: "Verified Rs. 50 in bank statement",
      verifiedByAdminEmail: ADMIN_1_EMAIL,
      updatedAt: new Date(),
    })
  );
});

test("PII Isolation, Admin Points Adjustment & Shadow Update Guard on users/{userId}", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const admin2Db = testEnv.authenticatedContext(ADMIN_2_UID, {
    email: ADMIN_2_EMAIL_MIXED,
    email_verified: true,
  }).firestore();
  const now = new Date();

  const validProfile = {
    userId: ALICE_UID,
    email: ALICE_EMAIL,
    displayName: "Alice",
    studentClass: 10,
    language: "en",
    xp: 100,
    totalPoints: 100,
    level: 1,
    quizzesPlayed: 1,
    gamesPlayed: 0,
    totalQuestionsAnswered: 10,
    totalCorrectAnswers: 8,
    badges: [],
    soundEnabled: true,
    lastActiveDate: "2026-11-03",
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 200,
      isAdmin: true,
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    admin2Db.collection("users").doc(ALICE_UID).update({
      totalPoints: 150,
      xp: 150,
      updatedAt: new Date(),
    })
  );
});

test("Customer Support: strict 1-to-1 Student-Admin isolation & both Admins can manage inbox", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const admin2Db = testEnv.authenticatedContext(ADMIN_2_UID, {
    email: ADMIN_2_EMAIL_MIXED,
    email_verified: true,
  }).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("support_threads").doc(ALICE_UID).set({
      studentUid: ALICE_UID,
      studentName: "Alice",
      studentEmail: ALICE_EMAIL,
      studentClass: 10,
      lastMessage: "Need help with subscription",
      unreadByAdmin: true,
      unreadByStudent: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("support_threads").doc(ALICE_UID).collection("messages").doc("msg_1").set({
      studentUid: ALICE_UID,
      senderUid: ALICE_UID,
      senderRole: "student",
      senderName: "Alice",
      text: "Hello Sir",
      createdAt: now,
    })
  );

  await assertFails(bobDb.collection("support_threads").doc(ALICE_UID).collection("messages").get());
  await assertFails(
    bobDb.collection("support_threads").doc(ALICE_UID).collection("messages").doc("msg_2").set({
      studentUid: ALICE_UID,
      senderUid: BOB_UID,
      senderRole: "student",
      senderName: "Bob",
      text: "Hi Alice",
      createdAt: now,
    })
  );

  await assertSucceeds(admin2Db.collection("support_threads").get());
  await assertSucceeds(
    admin2Db.collection("support_threads").doc(ALICE_UID).update({
      unreadByAdmin: false,
      unreadByStudent: true,
      lastMessage: "Your pass is now active!",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    admin2Db.collection("support_threads").doc(ALICE_UID).collection("messages").doc("msg_admin").set({
      studentUid: ALICE_UID,
      senderUid: ADMIN_2_UID,
      senderRole: "admin",
      senderName: "Pawan Admin",
      text: "Your pass is now active!",
      createdAt: now,
    })
  );
});

test("Question Manager App (/question-manager): Role isolation, Draft -> Review -> Main Admin Approval, and Student visibility", async () => {
  const admin1Db = testEnv.authenticatedContext(ADMIN_1_UID, {
    email: ADMIN_1_EMAIL,
    email_verified: true,
  }).firestore();
  const qmDb = testEnv.authenticatedContext(QM_UID, {
    email: QM_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: ALICE_EMAIL,
    email_verified: true,
  }).firestore();
  const now = new Date();

  // 1. Main Admin authorizes Question Manager (canPublishDirectly = false, canDeleteQuestions = true)
  await assertSucceeds(
    admin1Db.collection("allowed_question_managers").doc(QM_EMAIL_KEY).set({
      email: QM_EMAIL,
      managerName: "Teacher Sharma",
      isActive: true,
      canPublishDirectly: false,
      canDeleteQuestions: true,
      addedByAdminEmail: ADMIN_1_EMAIL,
      notes: "Mathematics & Science faculty",
      createdAt: now,
      updatedAt: now,
    })
  );

  // 2. Question Manager reads own allowlist record and syncs /question_managers/{uid} (cannot escalate to 'admin')
  await assertSucceeds(qmDb.collection("allowed_question_managers").doc(QM_EMAIL_KEY).get());
  await assertFails(
    qmDb.collection("question_managers").doc(QM_UID).set({
      uid: QM_UID,
      email: QM_EMAIL,
      name: "Teacher Sharma",
      role: "admin",
      emailKey: QM_EMAIL_KEY,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    qmDb.collection("question_managers").doc(QM_UID).set({
      uid: QM_UID,
      email: QM_EMAIL,
      name: "Teacher Sharma",
      role: "question_manager",
      emailKey: QM_EMAIL_KEY,
      createdAt: now,
      updatedAt: now,
    })
  );

  // 3. Question Manager MUST NOT receive Main Admin permissions
  await assertFails(qmDb.collection("allowed_students").get());
  await assertFails(qmDb.collection("subscription_records").get());
  await assertFails(qmDb.collection("admin_audit_logs").get());
  await assertFails(qmDb.collection("support_threads").get());
  await assertFails(qmDb.collection("allowed_question_managers").get());
  await assertFails(
    qmDb.collection("subject_catalog").doc("global").set({
      classes1To5: ["Mathematics"],
      classes6To8: ["Mathematics"],
      classes9To10: ["Mathematics"],
      classes11To12: ["Mathematics"],
      updatedByEmail: QM_EMAIL,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Main Admin CAN configure subject catalog
  await assertSucceeds(
    admin1Db.collection("subject_catalog").doc("global").set({
      classes1To5: ["Mathematics", "English", "Hindi", "EVS", "General Knowledge"],
      classes6To8: ["Mathematics", "Science", "English", "Hindi", "Social Science", "General Knowledge"],
      classes9To10: ["Mathematics", "Science", "Social Science", "English", "Hindi"],
      classes11To12: ["Physics", "Chemistry", "Biology", "Mathematics", "Accountancy", "Business Studies", "Economics", "History", "Geography", "Political Science", "English"],
      updatedByEmail: ADMIN_1_EMAIL,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(qmDb.collection("subject_catalog").doc("global").get());

  // 4. Question Manager without direct-publish permission CANNOT publish directly or upload http:// insecure image
  const baseQuestion = {
    authorId: QM_UID,
    authorEmail: QM_EMAIL,
    studentClass: 10,
    subject: "Mathematics",
    topic: "Quadratic Equations",
    language: "en",
    questionText: "What is the discriminant of ax^2 + bx + c = 0?",
    options: ["b^2 - 4ac", "b^2 + 4ac", "2a - b", "4ac - b"],
    correctIndex: 0,
    explanation: "The discriminant D is b^2 - 4ac.",
    difficulty: "medium",
    points: 10,
    timerSeconds: 30,
    imageUrl: "",
    isPublished: false,
    reviewStatus: "draft",
    reviewNotes: "",
    lastEditedByUid: QM_UID,
    lastEditedByEmail: QM_EMAIL,
    source: "manual",
    attemptsCount: 0,
    correctCount: 0,
    totalResponseTimeSec: 0,
    createdAt: now,
    updatedAt: now,
  };

  await assertFails(
    qmDb.collection("questions").doc("q_direct_pub").set({
      ...baseQuestion,
      isPublished: true,
      reviewStatus: "approved",
    })
  );

  await assertFails(
    qmDb.collection("questions").doc("q_bad_img").set({
      ...baseQuestion,
      imageUrl: "http://insecure.example.com/pic.png",
    })
  );

  // 5. Question Manager saves Draft, submits for Review, and records immutable QuestionAuditLog
  await assertSucceeds(qmDb.collection("questions").doc("q_1").set(baseQuestion));
  await assertSucceeds(
    qmDb.collection("questions").doc("q_1").update({
      reviewStatus: "pending_review",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    qmDb.collection("question_audit_logs").doc("qlog_1").set({
      editorUid: QM_UID,
      editorEmail: QM_EMAIL,
      editorRole: "question_manager",
      actionType: "submit_review",
      questionId: "q_1",
      questionSummary: "Class 10 Mathematics - Quadratic Equations",
      details: "Submitted draft question for Main Admin review",
      createdAt: now,
    })
  );
  await assertFails(qmDb.collection("question_audit_logs").doc("qlog_1").delete());

  // 6. Student CANNOT read draft/pending_review question or create questions
  await assertFails(aliceDb.collection("questions").doc("q_1").get());
  await assertFails(aliceDb.collection("questions").get());
  await assertFails(
    aliceDb.collection("questions").doc("q_student_hack").set({
      ...baseQuestion,
      authorId: ALICE_UID,
    })
  );

  // 7. Main Admin rejects a second draft (remains invisible to students) and approves+publishes q_1
  await assertSucceeds(qmDb.collection("questions").doc("q_rejected").set(baseQuestion));
  await assertSucceeds(
    admin1Db.collection("questions").doc("q_rejected").update({
      isPublished: false,
      reviewStatus: "rejected",
      reviewNotes: "Duplicate formula question",
      lastEditedByUid: ADMIN_1_UID,
      lastEditedByEmail: ADMIN_1_EMAIL,
      updatedAt: new Date(),
    })
  );
  // Invariant: rejected question can NEVER have isPublished: true even if Admin accidentally tries
  await assertFails(
    admin1Db.collection("questions").doc("q_rejected").update({
      isPublished: true,
      reviewStatus: "rejected",
      updatedAt: new Date(),
    })
  );
  await assertFails(aliceDb.collection("questions").doc("q_rejected").get());

  // Main Admin approves & publishes q_1 -> Student immediately sees ONLY q_1!
  await assertSucceeds(
    admin1Db.collection("questions").doc("q_1").update({
      isPublished: true,
      reviewStatus: "approved",
      reviewNotes: "Verified accurate syllabus question",
      lastEditedByUid: ADMIN_1_UID,
      lastEditedByEmail: ADMIN_1_EMAIL,
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.collection("questions").doc("q_1").get());
  const studentFeed = await assertSucceeds(aliceDb.collection("questions").where("isPublished", "==", true).get());
  if (studentFeed.docs.length !== 1 || studentFeed.docs[0].id !== "q_1") {
    throw new Error("Expected student feed to contain strictly the 1 approved+published question q_1");
  }

  // 8. Synchronous revocation: if Main Admin suspends Question Manager (isActive = false), write access stops immediately
  await assertSucceeds(
    admin1Db.collection("allowed_question_managers").doc(QM_EMAIL_KEY).update({
      isActive: false,
      updatedAt: new Date(),
    })
  );
  await assertFails(qmDb.collection("questions").doc("q_after_revoke").set(baseQuestion));
});
