import { useState, useEffect, useRef, useCallback } from "react";

export function useFormDraft(key, defaultValues, options = {}) {
  const { storage = "session" } = options;
  const storageArea = storage === "local" ? window.localStorage : window.sessionStorage;
  const storageKey = `policymesh:${key}:draft`;

  // Read initial draft from storage, fallback to defaultValues
  const [values, setValuesState] = useState(() => {
    try {
      const saved = storageArea.getItem(storageKey);
      if (saved) {
        return JSON.parse(saved);
      }
    } catch (e) {
      console.warn(`Failed to read draft for ${key}:`, e);
    }
    return defaultValues;
  });

  const [isDirty, setIsDirty] = useState(false);
  const debounceTimerRef = useRef(null);

  // Debounced save to storage
  const persistDraft = useCallback(
    (newValues) => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
      debounceTimerRef.current = setTimeout(() => {
        try {
          storageArea.setItem(storageKey, JSON.stringify(newValues));
        } catch (e) {
          console.warn(`Failed to persist draft for ${key}:`, e);
        }
      }, 300);
    },
    [storageArea, storageKey, key]
  );

  const setValues = useCallback(
    (updater) => {
      setValuesState((prev) => {
        const next = typeof updater === "function" ? updater(prev) : updater;
        setIsDirty(true);
        persistDraft(next);
        return next;
      });
    },
    [persistDraft]
  );

  const updateField = useCallback(
    (field, value) => {
      setValues((prev) => ({
        ...prev,
        [field]: value,
      }));
    },
    [setValues]
  );

  const clearDraft = useCallback(() => {
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }
    try {
      storageArea.removeItem(storageKey);
    } catch (e) {}
    setIsDirty(false);
    setValuesState(defaultValues);
  }, [storageArea, storageKey, defaultValues]);

  const resetForm = useCallback(() => {
    clearDraft();
  }, [clearDraft]);

  useEffect(() => {
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, []);

  return {
    values,
    setValues,
    updateField,
    clearDraft,
    resetForm,
    isDirty,
  };
}
