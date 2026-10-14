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

test("Unauthenticated user: cannot read transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Cross-user isolation: Bob cannot read Alice's transactions", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 50000,
      category: "Makanan",
      wallet: "Tunai",
      dateMillis: Date.now(),
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").get());
});

test("Authenticated owner: Alice can read her own transactions", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 50000,
      category: "Makanan",
      wallet: "Tunai",
      dateMillis: Date.now(),
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Authenticated owner: Alice can create a valid transaction", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_new").set({
      userId: ALICE_UID,
      type: "INCOME",
      amount: 5000000,
      category: "Gaji",
      wallet: "Rekening Bank",
      note: "Gaji Bulanan",
      dateMillis: Date.now(),
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Integrity check: Alice cannot create transaction with spoofed userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_spoof").set({
      userId: BOB_UID,
      type: "EXPENSE",
      amount: 10000,
      category: "Transportasi",
      wallet: "Tunai",
      dateMillis: Date.now(),
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Validation check: Alice cannot create transaction with negative amount", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_neg").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: -5000,
      category: "Makanan",
      wallet: "Tunai",
      dateMillis: Date.now(),
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});
