/**
 * Formats a number using the Indian numbering system, e.g. 1234567 -> "12,34,567.00"
 * Prefix with ₹ where needed by the caller.
 */
function formatInr(amount) {
    const num = Number(amount || 0);
    return num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatInrWithSymbol(amount) {
    return '₹' + formatInr(amount);
}
