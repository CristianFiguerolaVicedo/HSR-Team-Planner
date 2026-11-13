import { useEffect, useRef } from "react";
import { X } from "lucide-react";

/**
 * Native <dialog> as a modal: focus trap, Esc and backdrop come from the browser.
 * Same rules as CharacterDetail: showModal() on mount, and no close() on unmount (removing the
 * element already closes it, and under StrictMode a close() would fire a late "close" event).
 */
const Modal = ({ title, onClose, children, footer }) => {
  const dialogRef = useRef(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (dialog && !dialog.open) dialog.showModal();
  }, []);

  const onBackdropClick = (e) => {
    if (e.target === dialogRef.current) onClose();
  };

  return (
    <dialog ref={dialogRef} className="detail-dialog" aria-labelledby="modal-title" onClose={onClose} onClick={onBackdropClick}>
      <div className="flex flex-col max-h-[inherit]">
        <div className="flex items-center justify-between gap-3 px-5 pt-4 pb-3 border-b border-border">
          <h2 id="modal-title" className="text-xl font-extrabold text-text-primary">{title}</h2>
          <button type="button" onClick={onClose} aria-label="Close" className="p-1.5 rounded-lg text-text-muted hover:text-text-primary hover:bg-bg-elevated">
            <X size={20} />
          </button>
        </div>
        <div className="overflow-y-auto px-5 py-4">{children}</div>
        {footer && <div className="px-5 py-3 border-t border-border">{footer}</div>}
      </div>
    </dialog>
  );
};

export default Modal;
