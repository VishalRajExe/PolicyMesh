# Scenario 05 — AI Classification

## What Happens

The AI classification service analyzes schema fields from a customer and payment schema, suggesting data class labels for each field. A human must approve or override each suggestion before it is used for enforcement.

## Input

Two schemas are submitted to the AI classification service:

### Customer Schema
| Field | Type |
|-------|------|
| customerId | string |
| name | string |
| email | string |
| phone | string |
| address | string |
| createdAt | datetime |

### Payment Schema
| Field | Type |
|-------|------|
| paymentId | string |
| orderId | string |
| cardNumber | string |
| expiryDate | string |
| paymentStatus | enum |

## Expected AI Suggestions

| Field | Classification | Confidence | Reason |
|-------|---------------|------------|--------|
| customerId | NON_SENSITIVE | 0.94 | Operational identifier |
| name | PII | 0.97 | Identifies an individual |
| email | PII | 0.97 | Contact information for an individual |
| phone | PII | 0.97 | Contact information for an individual |
| address | PII | 0.97 | Location data for an individual |
| createdAt | NON_SENSITIVE | 0.94 | Operational timestamp |
| paymentId | NON_SENSITIVE | 0.94 | Operational identifier |
| orderId | NON_SENSITIVE | 0.94 | Operational identifier |
| cardNumber | PCI | 0.99 | Payment card information |
| expiryDate | PCI | 0.99 | Payment card information |
| paymentStatus | NON_SENSITIVE | 0.94 | Operational status value |

## Result

```
🤖 AI Classification Complete
⚠️  Human approval required
```

## ⚠️ Important Disclaimer

**AI classification is only a suggestion. Human approval is required.**

The AI service is a productivity tool that accelerates the initial classification step. It does NOT make enforcement decisions. Every classification must be:

1. **Reviewed** by a human operator
2. **Approved** or **overridden** before it takes effect
3. **Validated** against organizational policies

The AI may make mistakes. A field classified as `NON_SENSITIVE` might actually contain sensitive data in context. The AI might miss sensitive fields that depend on application logic to identify.

## Why This Matters

Manual classification of every field in every schema is tedious and error-prone. The AI service provides a starting point that speeds up the process while keeping a human in the loop for quality assurance.

## How to Reproduce

```bash
curl -X POST http://localhost:8000/api/v1/classify \
  -H "Content-Type: application/json" \
  -d @examples/demo/scenario-05-ai-classification/input.json
```
