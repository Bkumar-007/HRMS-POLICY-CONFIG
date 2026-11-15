import Icon from "./Icon";

const VARIANT_CLASS = {
  primary: "btn btn-primary",
  secondary: "btn btn-secondary",
  danger: "btn btn-danger",
  ghost: "btn btn-ghost",
  link: "btn btn-link",
};

export default function Button({
  children,
  variant = "primary",
  icon,
  iconPosition = "left",
  type = "button",
  className = "",
  fullWidth = false,
  loading = false,
  disabled = false,
  ...rest
}) {
  const classes = [
    VARIANT_CLASS[variant] || VARIANT_CLASS.primary,
    fullWidth ? "btn-block" : "",
    icon && !children ? "btn-icon-only" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button type={type} className={classes} disabled={disabled || loading} {...rest}>
      {loading ? (
        <span className="btn-spinner" aria-hidden="true" />
      ) : (
        <>
          {icon && iconPosition === "left" ? <Icon name={icon} size={16} /> : null}
          {children ? <span>{children}</span> : null}
          {icon && iconPosition === "right" ? <Icon name={icon} size={16} /> : null}
        </>
      )}
    </button>
  );
}
