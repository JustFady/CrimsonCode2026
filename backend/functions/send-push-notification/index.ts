/**
 * Send Push Notification Edge Function
 *
 * Sends FCM (Firebase Cloud Messaging) notifications to event recipients.
 * This function is invoked when a new private event is created to notify
 * matched contacts via push notifications.
 *
 * Spec requirements:
 * - Send FCM notifications to event recipients with event data payload
 * - Only used for private events (public events have no push fanout in MVP)
 * - Payload includes: event_id, severity, category, description, lat, lon, deep_link
 */

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.38.4"

/**
 * FCM notification payload structure
 * Matches PushNotificationPayload model in mobile app
 */
interface FcmNotificationPayload {
  event_id: string
  severity: string
  category: string
  description: string
  lat?: number | null
  lon?: number | null
  deep_link: string
}

/**
 * FCM API message request structure
 */
interface FcmMessage {
  to: string
  notification: {
    title: string
    body: string
  }
  data: Record<string, string | number | boolean | null>
  android?: {
    channel_id?: string
    priority?: string
    notification?: {
      notification_priority?: string
      vibration_pattern?: number[]
    }
  }
  apns?: {
    headers?: {
      "apns-priority"?: string
    }
    payload?: {
      aps: {
        alert: {
          title: string
          body: string
        }
        sound?: string
        badge?: number
      }
    }
  }
}

/**
 * Request body for this edge function
 */
interface SendNotificationRequest {
  event_id: string
  severity: string
  category: string
  description: string
  lat?: number | null
  lon?: number | null
}

/**
 * Response structure
 */
interface SuccessResponse {
  success: true
  notifications_sent: number
  recipients: Array<{ user_id: string; fcm_token: string }>
}

interface ErrorResponse {
  success: false
  error: string
}

/**
 * Supabase service role client for admin operations
 */
const supabaseUrl = Deno.env.get("SUPABASE_URL")!
const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
const fcmServerKey = Deno.env.get("FCM_SERVER_KEY")!

const supabase = createClient(supabaseUrl, supabaseServiceKey)

/**
 * Send FCM notification to a single device
 */
async function sendFcmNotification(
  fcmToken: string,
  payload: FcmNotificationPayload
): Promise<{ success: boolean; error?: string }> {
  const isCrisis = payload.severity === "CRISIS"

  // Build notification title: "Severity - Category" format
  const title = `${payload.severity} - ${payload.category}`

  const fcmMessage: FcmMessage = {
    to: fcmToken,
    notification: {
      title: title,
      body: payload.description,
    },
    data: {
      event_id: payload.event_id,
      severity: payload.severity,
      category: payload.category,
      description: payload.description,
      lat: payload.lat ?? null,
      lon: payload.lon ?? null,
      deep_link: payload.deep_link,
      view_on_map: "true",
    },
    // Android-specific settings
    android: {
      channel_id: isCrisis ? "emergency_alerts" : "alerts",
      priority: isCrisis ? "high" : "normal",
      notification: {
        notification_priority: isCrisis ? "PRIORITY_HIGH" : "PRIORITY_NORMAL",
        // Aggressive vibration pattern for crisis: [duration_ms, sleep_ms, ...]
        vibration_pattern: isCrisis ? [0, 400, 200, 400, 200, 400] : [0, 250, 250],
      },
    },
    // iOS-specific settings
    apns: {
      headers: {
        "apns-priority": isCrisis ? "10" : "5",
      },
      payload: {
        aps: {
          alert: {
            title: title,
            body: payload.description,
          },
          sound: isCrisis ? "default" : "default",
          badge: 1,
        },
      },
    },
  }

  try {
    const response = await fetch(
      "https://fcm.googleapis.com/fcm/send",
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `key=${fcmServerKey}`,
        },
        body: JSON.stringify(fcmMessage),
      }
    )

    const result = await response.json()

    if (response.ok) {
      return { success: true }
    } else {
      console.error(
        `FCM send failed to ${fcmToken.slice(0, 10)}...:`,
        result
      )
      return { success: false, error: JSON.stringify(result) }
    }
  } catch (error) {
    console.error(`FCM send error:`, error)
    return { success: false, error: String(error) }
  }
}

/**
 * Main edge function handler
 */
serve(async (req) => {
  // Only accept POST requests
  if (req.method !== "POST") {
    return new Response(
      JSON.stringify({
        success: false,
        error: "Method not allowed. Use POST.",
      } as ErrorResponse),
      {
        status: 405,
        headers: { "Content-Type": "application/json" },
      }
    )
  }

  try {
    // Parse request body
    const body: SendNotificationRequest = await req.json()

    // Validate required fields
    if (!body.event_id || !body.severity || !body.category) {
      return new Response(
        JSON.stringify({
          success: false,
          error:
            "Missing required fields: event_id, severity, category",
        } as ErrorResponse),
        {
          status: 400,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    // Validate severity
    if (
      body.severity !== "ALERT" &&
      body.severity !== "CRISIS"
    ) {
      return new Response(
        JSON.stringify({
          success: false,
          error: "Invalid severity. Must be ALERT or CRISIS.",
        } as ErrorResponse),
        {
          status: 400,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    // Validate severity
    if (!body.severity || !["ALERT", "CRISIS"].includes(body.severity)) {
      return new Response(
        JSON.stringify({
          success: false,
          error: "Invalid severity. Must be ALERT or CRISIS.",
        } as ErrorResponse),
        {
          status: 400,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    // Query event_recipients to get recipient user IDs
    const { data: recipients, error: recipientsError } = await supabase
      .from("event_recipients")
      .select("user_id")
      .eq("event_id", body.event_id)

    if (recipientsError) {
      console.error("Error fetching event_recipients:", recipientsError)
      return new Response(
        JSON.stringify({
          success: false,
          error: "Failed to fetch event recipients",
        } as ErrorResponse),
        {
          status: 500,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    if (!recipients || recipients.length === 0) {
      return new Response(
        JSON.stringify({
          success: true,
          notifications_sent: 0,
          recipients: [],
        } as SuccessResponse),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    // Get FCM tokens for recipients
    const userIds = recipients.map((r) => r.user_id)
    const { data: users, error: usersError } = await supabase
      .from("users")
      .select("id, fcm_token")
      .in("id", userIds)
      .not("fcm_token", "is", null)

    if (usersError) {
      console.error("Error fetching users:", usersError)
      return new Response(
        JSON.stringify({
          success: false,
          error: "Failed to fetch user tokens",
        } as ErrorResponse),
        {
          status: 500,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    if (!users || users.length === 0) {
      return new Response(
        JSON.stringify({
          success: true,
          notifications_sent: 0,
          recipients: [],
        } as SuccessResponse),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    // Build notification payload
    const notificationPayload: FcmNotificationPayload = {
      event_id: body.event_id,
      severity: body.severity,
      category: body.category,
      description: body.description || "",
      lat: body.lat ?? null,
      lon: body.lon ?? null,
      deep_link: `crimsoncode://event/${body.event_id}`,
    }

    // Send notifications to all recipients (in parallel for efficiency)
    const sendPromises = users.map((user) =>
      sendFcmNotification(user.fcm_token, notificationPayload)
    )

    const results = await Promise.allSettled(sendPromises)

    // Count successful sends
    let successCount = 0
    const recipientsWithTokens = users.map((user) => ({
      user_id: user.id,
      fcm_token: user.fcm_token,
    }))

    results.forEach((result) => {
      if (
        result.status === "fulfilled" &&
        result.value.success
      ) {
        successCount++
      }
    })

    return new Response(
      JSON.stringify({
        success: true,
        notifications_sent: successCount,
        recipients: recipientsWithTokens,
      } as SuccessResponse),
      {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }
    )
  } catch (error) {
    console.error("Unexpected error:", error)
    return new Response(
      JSON.stringify({
        success: false,
        error: "Internal server error",
      } as ErrorResponse),
      {
        status: 500,
        headers: { "Content-Type": "application/json" },
      }
    )
  }
})
