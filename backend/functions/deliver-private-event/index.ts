/**
 * Deliver Private Event Edge Function
 *
 * Handles private event delivery by:
 * - Querying creator's contact list from user_contacts table
 * - Matching contact phone numbers to registered users
 * - Creating EventRecipient records for each matched user
 * - Calling send-push-notification to send FCM notifications
 *
 * This is critical for private events to work properly.
 */

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.38.4"

/**
 * Request body for this edge function
 */
interface DeliverPrivateEventRequest {
  event_id: string
  severity: string
  category: string
  description: string
  lat?: number | null
  lon?: number | null
}

/**
 * Contact from user_contacts table
 */
interface Contact {
  contact_phone_number: string | null
  contact_user_id: string | null
  display_name: string | null
}

/**
 * User from users table
 */
interface User {
  id: string
  phone_number: string
}

/**
 * Event from events table
 */
interface Event {
  id: string
  creator_id: string
  broadcast_type: string
}

/**
 * Response structure
 */
interface SuccessResponse {
  success: true
  event_id: string
  recipients_matched: number
  event_recipients_created: number
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

const supabase = createClient(supabaseUrl, supabaseServiceKey)

/**
 * Call send-push-notification edge function
 */
async function sendPushNotification(
  payload: DeliverPrivateEventRequest
): Promise<{ success: boolean; error?: string }> {
  try {
    const response = await fetch(
      `${supabaseUrl}/functions/v1/send-push-notification`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${supabaseServiceKey}`,
        },
        body: JSON.stringify(payload),
      }
    )

    const result = await response.json()

    if (response.ok) {
      return { success: true }
    } else {
      console.error("send-push-notification failed:", result)
      return { success: false, error: JSON.stringify(result) }
    }
  } catch (error) {
    console.error("send-push-notification error:", error)
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
    const body: DeliverPrivateEventRequest = await req.json()

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

    // Query the event to get creator_id and verify broadcast_type
    const { data: event, error: eventError } = await supabase
      .from("events")
      .select("id, creator_id, broadcast_type")
      .eq("id", body.event_id)
      .single()

    if (eventError || !event) {
      console.error("Error fetching event:", eventError)
      return new Response(
        JSON.stringify({
          success: false,
          error: "Event not found",
        } as ErrorResponse),
        {
          status: 404,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    // Verify this is a private event
    if (event.broadcast_type !== "PRIVATE") {
      return new Response(
        JSON.stringify({
          success: false,
          error: "Event is not private. This function only handles PRIVATE events.",
        } as ErrorResponse),
        {
          status: 400,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    // Query creator's user_contacts
    const { data: contacts, error: contactsError } = await supabase
      .from("user_contacts")
      .select("contact_phone_number, contact_user_id, display_name")
      .eq("user_id", event.creator_id)

    if (contactsError) {
      console.error("Error fetching user_contacts:", contactsError)
      return new Response(
        JSON.stringify({
          success: false,
          error: "Failed to fetch user contacts",
        } as ErrorResponse),
        {
          status: 500,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    if (!contacts || contacts.length === 0) {
      // No contacts to deliver to - success with 0 recipients
      return new Response(
        JSON.stringify({
          success: true,
          event_id: body.event_id,
          recipients_matched: 0,
          event_recipients_created: 0,
        } as SuccessResponse),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    // Match contacts to registered users
    const matchedUserIds: string[] = []

    for (const contact of contacts) {
      // Skip contacts without phone number
      if (!contact.contact_phone_number) {
        continue
      }

      // If contact_user_id is populated, use it directly
      if (contact.contact_user_id) {
        matchedUserIds.push(contact.contact_user_id)
        continue
      }

      // Otherwise, look up user by phone number
      const { data: user, error: userError } = await supabase
        .from("users")
        .select("id, phone_number")
        .eq("phone_number", contact.contact_phone_number)
        .single()

      if (!userError && user) {
        matchedUserIds.push(user.id)
      }
    }

    if (matchedUserIds.length === 0) {
      // No contacts have the app installed
      return new Response(
        JSON.stringify({
          success: true,
          event_id: body.event_id,
          recipients_matched: 0,
          event_recipients_created: 0,
        } as SuccessResponse),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    // Create EventRecipient records for each matched user
    const eventRecipientRows = matchedUserIds.map((userId) => ({
      event_id: body.event_id,
      user_id: userId,
      delivery_status: "PENDING",
      notified_at: null,
    }))

    const { error: insertError } = await supabase
      .from("event_recipients")
      .insert(eventRecipientRows, { count: "exact" })

    if (insertError) {
      console.error("Error inserting event_recipients:", insertError)
      return new Response(
        JSON.stringify({
          success: false,
          error: "Failed to create event recipients",
        } as ErrorResponse),
        {
          status: 500,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    // Call send-push-notification to deliver notifications
    const notificationResult = await sendPushNotification(body)

    return new Response(
      JSON.stringify({
        success: true,
        event_id: body.event_id,
        recipients_matched: matchedUserIds.length,
        event_recipients_created: matchedUserIds.length,
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
