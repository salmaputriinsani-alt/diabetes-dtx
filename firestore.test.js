const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function buildValidProfile(uid, ts) {
  return {
    userId: uid,
    patientName: "Encik Ahmad",
    age: 46,
    diagnosisTrack: "EARLY_T2D_REVERSAL",
    language: "en",
    sensorDaysLeft: 9,
    sensorBattery: 82,
    targetLowMgDl: 70,
    targetHighMgDl: 180,
    sleepHours: 4.5,
    walkMinutes: 15,
    simMealCarbs: 45,
    simInsulinUnits: 1.5,
    neuroScore: 82,
    renalEgfr: 94,
    renalUacr: 12.4,
    createdAt: ts,
    updatedAt: ts,
  };
}

function buildValidLog(uid, ts) {
  return {
    userId: uid,
    entryType: "MEAL",
    title: "Nasi Lemak + Teh O",
    timeLabel: "08:50",
    dateGroup: "Today",
    glucoseMgDl: 119,
    carbsGrams: 52,
    proteinGrams: 16,
    fatGrams: 18,
    fiberGrams: 4,
    glycemicLoad: "High",
    insulinUnits: 0.0,
    shapImpactSummary: "Meal Carbs +30 mg/dL",
    habitTip: "Swap half coconut rice for steamed cauliflower rice or add extra cucumber.",
    createdAt: ts,
    updatedAt: ts,
  };
}

function buildValidMessage(uid, ts) {
  return {
    userId: uid,
    senderRole: "PATIENT",
    senderName: "Encik Ahmad",
    text: "Dr Sarah, my post-breakfast reading reached 188 mg/dL. Should I adjust?",
    timeLabel: "09:24",
    attachedGraphSummary: "188 now | 1x meal | 1.5 unit estimate",
    attachedEstimateUnits: 1.5,
    createdAt: ts,
    updatedAt: ts,
  };
}

function buildValidScreening(uid, ts) {
  return {
    userId: uid,
    footTingling: false,
    hrvStressHigh: true,
    postSpikeBrainFog: false,
    neuroStabilityScore: 82,
    systolicBp: 124,
    diastolicBp: 78,
    hydrationGlasses: 7,
    egfr: 94,
    uacr: 12.4,
    createdAt: ts,
    updatedAt: ts,
  };
}

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

test("Unauthenticated users cannot read or write any AURELIX collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const pastTs = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(buildValidProfile(ALICE_UID, pastTs))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("logs").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("doctorMessages").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("screenings").get()
  );
});

test("Owner can create, read, and partially update PatientProfile while rejecting ghost/future fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(buildValidProfile(ALICE_UID, created))
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Partial update on simulator & language fields
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      language: "ms",
      sleepHours: 6.5,
      walkMinutes: 25,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set(
      buildValidProfile(BOB_UID, new Date(Date.now() + 60000))
    )
  );
});

test("Owner can create, query, and update MetabolicLogs, DoctorMessages, and OrganScreenings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 4000);

  // Logs
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("logs")
      .doc("log_1")
      .set(buildValidLog(ALICE_UID, created))
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("logs")
      .orderBy("createdAt", "desc")
      .get()
  );

  // Reject @DocumentId `id` field inside payload
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("logs")
      .doc("log_2")
      .set({ ...buildValidLog(ALICE_UID, created), id: "log_2" })
  );

  // DoctorMessages
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("doctorMessages")
      .doc("msg_1")
      .set(buildValidMessage(ALICE_UID, created))
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("doctorMessages")
      .doc("msg_1")
      .update({
        confirmedBadge: "Confirmed by Doctor - 1 unit",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Screenings
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("screenings")
      .doc("scr_1")
      .set(buildValidScreening(ALICE_UID, created))
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("screenings")
      .doc("scr_1")
      .update({
        hydrationGlasses: 8,
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Cross-user isolation: Bob cannot read, list, or mutate Alice's medical data", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const ts = new Date(Date.now() - 5000);
    await db.collection("users").doc(ALICE_UID).set(buildValidProfile(ALICE_UID, ts));
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("logs")
      .doc("log_1")
      .set(buildValidLog(ALICE_UID, ts));
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("doctorMessages")
      .doc("msg_1")
      .set(buildValidMessage(ALICE_UID, ts));
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("screenings")
      .doc("scr_1")
      .set(buildValidScreening(ALICE_UID, ts));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("logs").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("doctorMessages").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("screenings").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("logs")
      .doc("log_1")
      .update({ title: "Tampered", updatedAt: new Date(Date.now() - 1000) })
  );
});
