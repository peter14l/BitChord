package blend

import (
	"strings"
	"testing"
	"time"
)

func TestBlendStoreLifecycle(t *testing.T) {
	store := NewStore()

	userAPayload := "mock_gzip_base64_alex_taste_profile"
	userBPayload := "mock_gzip_base64_jordan_taste_profile"

	// 1. Create blend session
	session, err := store.Create(userAPayload)
	if err != nil {
		t.Fatalf("failed to create blend session: %v", err)
	}
	if len(session.Code) != 6 {
		t.Fatalf("expected 6-character code, got %s", session.Code)
	}

	// 2. Check status (should be false, User B has not joined)
	hasJoined, err := store.GetStatus(session.Code)
	if err != nil || hasJoined {
		t.Fatalf("expected status false, got %v, err=%v", hasJoined, err)
	}

	// 3. User B joins concurrently
	errCh := make(chan error, 1)
	go func() {
		time.Sleep(50 * time.Millisecond)
		receivedA, err := store.Join(session.Code, userBPayload)
		if err != nil {
			errCh <- err
			return
		}
		if receivedA != userAPayload {
			t.Errorf("User B received wrong payload: got %s, want %s", receivedA, userAPayload)
		}
		errCh <- nil
	}()

	// 4. User A waits for User B
	receivedB, err := store.WaitForB(session.Code, 2*time.Second)
	if err != nil {
		t.Fatalf("WaitForB failed: %v", err)
	}
	if receivedB != userBPayload {
		t.Fatalf("User A received wrong payload: got %s, want %s", receivedB, userBPayload)
	}

	// Check background join error
	if joinErr := <-errCh; joinErr != nil {
		t.Fatalf("Join goroutine error: %v", joinErr)
	}

	// 5. Session should be cleaned up after exchange
	_, err = store.GetStatus(session.Code)
	if err != ErrSessionNotFound {
		t.Fatalf("expected ErrSessionNotFound after exchange, got %v", err)
	}
}

func TestBlendRejectsEmptyAndOversize(t *testing.T) {
	store := NewStore()

	_, err := store.Create("")
	if err != ErrEmptyPayload {
		t.Fatalf("expected ErrEmptyPayload, got %v", err)
	}

	hugePayload := strings.Repeat("a", MaxPayloadLength+10)
	_, err = store.Create(hugePayload)
	if err == nil {
		t.Fatalf("expected error for oversized payload, got nil")
	}
}
