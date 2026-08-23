import { forwardRef } from "react";
import { Loader2 } from "lucide-react";

export const Button = forwardRef(function Button(
  {
    children,
    variant = "primary", // primary, secondary, ghost, danger
    size = "md", // sm, md, lg
    loading = false,
    disabled = false,
    icon: Icon,
    iconRight: IconRight,
    className = "",
    type = "button",
    ...props
  },
  ref
) {
  const sizeClasses = {
    sm: "px-2.5 py-1.5 text-xs rounded-lg gap-1.5",
    md: "px-3.5 py-2 text-xs font-medium rounded-xl gap-2",
    lg: "px-4.5 py-2.5 text-sm font-medium rounded-xl gap-2.5",
  }[size] || "px-3.5 py-2 text-xs font-medium rounded-xl gap-2";

  const variantClasses = {
    primary: "btn-primary",
    secondary: "btn-secondary",
    ghost: "btn-ghost",
    danger: "btn-danger",
  }[variant] || "btn-primary";

  return (
    <button
      ref={ref}
      type={type}
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center transition-all duration-150 focus-ring select-none ${variantClasses} ${sizeClasses} ${className}`}
      {...props}
    >
      {loading ? (
        <Loader2 size={size === "sm" ? 13 : 15} className="animate-spin shrink-0" />
      ) : Icon ? (
        <Icon size={size === "sm" ? 13 : 15} className="shrink-0" />
      ) : null}
      {children}
      {!loading && IconRight && (
        <IconRight size={size === "sm" ? 13 : 15} className="shrink-0" />
      )}
    </button>
  );
});

export default Button;
