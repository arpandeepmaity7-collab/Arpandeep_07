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

test("Unauthenticated user cannot read or write tasks or categories", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("categories").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .set({
        userId: ALICE_UID,
        title: "Unauthorized task",
        notes: "",
        completed: false,
        priority: "HIGH",
        reminderEnabled: false,
        subtasks: [],
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Owner can create, list, update, and delete categories; cross-user blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("categories")
      .doc("cat_work")
      .set({
        userId: ALICE_UID,
        name: "Editorial",
        tone: "terracotta",
        createdAt: now,
        updatedAt: now,
      })
  );

  // List own categories (even empty or populated)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("categories").get()
  );

  // Update allowed fields
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("categories")
      .doc("cat_work")
      .update({
        name: "Work Studio",
        tone: "sage",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject invalid tone enum
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("categories")
      .doc("cat_bad")
      .set({
        userId: ALICE_UID,
        name: "Bad",
        tone: "neon_pink",
        createdAt: now,
        updatedAt: now,
      })
  );

  // Cross-user read & write blocked
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("categories").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("categories")
      .doc("cat_work")
      .delete()
  );
});

test("Owner can create Kotlin-mirrored tasks with subtasks, partial updates, and optional field clearing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Create referenced category first
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("categories")
      .doc("cat_design")
      .set({
        userId: ALICE_UID,
        name: "Design",
        tone: "indigo",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Create task with optional fields & subtasks (no @DocumentId 'id' field)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .set({
        userId: ALICE_UID,
        title: "Draft Q4 folio layout",
        notes: "Include serif typography specimens",
        completed: false,
        priority: "HIGH",
        dueAtMillis: Date.now() + 86400000,
        reminderAtMillis: Date.now() + 3600000,
        reminderEnabled: true,
        categoryId: "cat_design",
        subtasks: [
          { id: "sub_1", title: "Pick paper palette", completed: true },
          { id: "sub_2", title: "Set grid margins", completed: false },
        ],
        createdAt: created,
        updatedAt: created,
      })
  );

  // List own tasks
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").get()
  );

  // Partial update: toggle completion & reminder
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .update({
        completed: true,
        reminderEnabled: false,
        updatedAt: new Date(Date.now() - 2000),
      })
  );

  // Partial update: update subtasks checklist
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .update({
        subtasks: [
          { id: "sub_1", title: "Pick paper palette", completed: true },
          { id: "sub_2", title: "Set grid margins", completed: true },
        ],
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject ghost field / @DocumentId `id` field
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_2")
      .set({
        id: "task_2",
        userId: ALICE_UID,
        title: "Invalid ID field",
        notes: "",
        completed: false,
        priority: "LOW",
        reminderEnabled: false,
        subtasks: [],
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject orphaned categoryId
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_3")
      .set({
        userId: ALICE_UID,
        title: "Orphaned tag",
        notes: "",
        completed: false,
        priority: "MEDIUM",
        reminderEnabled: false,
        categoryId: "non_existent_cat",
        subtasks: [],
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .update({
        createdAt: new Date(Date.now() - 500),
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Reject future server timestamp spoofing
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_4")
      .set({
        userId: ALICE_UID,
        title: "Future timestamp",
        notes: "",
        completed: false,
        priority: "LOW",
        reminderEnabled: false,
        subtasks: [],
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );
});

test("Cross-user access to tasks is denied", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("alice_task")
      .set({
        userId: ALICE_UID,
        title: "Private task",
        notes: "",
        completed: false,
        priority: "MEDIUM",
        reminderEnabled: false,
        subtasks: [],
        createdAt: created,
        updatedAt: created,
      });
  });

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tasks").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("alice_task")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("alice_task")
      .update({
        completed: true,
        reminderEnabled: false,
        updatedAt: new Date(Date.now() - 1000),
      })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("alice_task")
      .delete()
  );
});
