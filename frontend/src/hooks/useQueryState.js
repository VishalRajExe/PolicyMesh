import { useSearchParams } from "react-router-dom";
import { useCallback, useMemo } from "react";

export function useQueryState(key, defaultValue) {
  const [searchParams, setSearchParams] = useSearchParams();

  const value = useMemo(() => {
    const param = searchParams.get(key);
    if (param === null) return defaultValue;
    if (typeof defaultValue === "number") {
      const parsed = Number(param);
      return isNaN(parsed) ? defaultValue : parsed;
    }
    return param;
  }, [searchParams, key, defaultValue]);

  const setValue = useCallback(
    (newValue) => {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          if (
            newValue === undefined ||
            newValue === null ||
            newValue === "" ||
            newValue === defaultValue ||
            newValue === "ALL"
          ) {
            next.delete(key);
          } else {
            next.set(key, String(newValue));
          }
          return next;
        },
        { replace: true }
      );
    },
    [setSearchParams, key, defaultValue]
  );

  return [value, setValue];
}
