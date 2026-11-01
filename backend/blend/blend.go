package blend

import (
	"errors"
	"strings"
	"sync"
	"time"

	"github.com/KabirSinghBhatia/BitChord/backend/codes"
)

var (
	ErrSessionNotFound = errors.New("blend session not found or expired")
	ErrSessionExpired  = errors.New("blend session has expired")
	ErrSessionFull     = errors.New("blend session already completed")
	ErrEmptyPayload    = errors.New("taste profile payload cannot be empty")
)

const (
	DefaultSessionTTL = 15 * time.Minute
	MaxPayloadLength  = 64 * 1024 // 64 KB limit for compressed profile payload
)

type Session struct {
	Code         string    `json:"code"`
	UserAPayload string    `json:"-"`
	UserBPayload string    `json:"-"`
	CreatedAt    time.Time `json:"createdAt"`
	ExpiresAt    time.Time `json:"expiresAt"`

	completedCh chan struct{}
	closeOnce   sync.Once
}

type Store struct {
	mu       sync.RWMutex
	sessions map[string]*Session
}

func NewStore() *Store {
	return &Store{
		sessions: make(map[string]*Session),
	}
}

// Create generates a new 6-character blend room and stores User A's taste profile payload.
func (s *Store) Create(userAPayload string) (*Session, error) {
	payload := strings.TrimSpace(userAPayload)
	if payload == "" {
		return nil, ErrEmptyPayload
	}
	if len(payload) > MaxPayloadLength {
		return nil, errors.New("payload exceeds maximum allowed size")
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	// Evict expired sessions periodically on write
	s.evictExpiredLocked()

	var code string
	for i := 0; i < 10; i++ {
		candidate := codes.NewCode()
		if _, exists := s.sessions[candidate]; !exists {
			code = candidate
			break
		}
	}
	if code == "" {
		return nil, errors.New("failed to generate unique blend code")
	}

	now := time.Now()
	session := &Session{
		Code:         code,
		UserAPayload: payload,
		CreatedAt:    now,
		ExpiresAt:    now.Add(DefaultSessionTTL),
		completedCh:  make(chan struct{}),
	}
	s.sessions[code] = session
	return session, nil
}

// Join accepts User B's taste profile, links it to the session, notifies User A, and returns User A's profile.
func (s *Store) Join(rawCode, userBPayload string) (string, error) {
	code := codes.Normalise(rawCode)
	payload := strings.TrimSpace(userBPayload)
	if payload == "" {
		return "", ErrEmptyPayload
	}
	if len(payload) > MaxPayloadLength {
		return "", errors.New("payload exceeds maximum allowed size")
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	session, exists := s.sessions[code]
	if !exists || time.Now().After(session.ExpiresAt) {
		delete(s.sessions, code)
		return "", ErrSessionNotFound
	}

	if session.UserBPayload != "" {
		return "", ErrSessionFull
	}

	session.UserBPayload = payload
	session.closeOnce.Do(func() {
		close(session.completedCh)
	})

	return session.UserAPayload, nil
}

// WaitForB waits for User B to join and deliver their profile, returning User B's profile.
// Upon success, the session is removed from memory.
func (s *Store) WaitForB(rawCode string, timeout time.Duration) (string, error) {
	code := codes.Normalise(rawCode)

	s.mu.RLock()
	session, exists := s.sessions[code]
	s.mu.RUnlock()

	if !exists || time.Now().After(session.ExpiresAt) {
		return "", ErrSessionNotFound
	}

	select {
	case <-session.completedCh:
		s.mu.Lock()
		defer s.mu.Unlock()
		bPayload := session.UserBPayload
		// Auto-delete on successful exchange
		delete(s.sessions, code)
		return bPayload, nil
	case <-time.After(timeout):
		return "", errors.New("timeout waiting for blend partner")
	}
}

// GetStatus checks if a session exists and is still awaiting User B.
func (s *Store) GetStatus(rawCode string) (bool, error) {
	code := codes.Normalise(rawCode)

	s.mu.RLock()
	defer s.mu.RUnlock()

	session, exists := s.sessions[code]
	if !exists || time.Now().After(session.ExpiresAt) {
		return false, ErrSessionNotFound
	}
	return session.UserBPayload != "", nil
}

func (s *Store) evictExpiredLocked() {
	now := time.Now()
	for code, sess := range s.sessions {
		if now.After(sess.ExpiresAt) {
			delete(s.sessions, code)
		}
	}
}
