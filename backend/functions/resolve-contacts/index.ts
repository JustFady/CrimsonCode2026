/**
 * Resolve Contacts Edge Function
 *
 * Matches phone numbers to registered app users.
 * Used for contact app user detection and badges.
 *
 * Returns phone numbers with corresponding user IDs.
 */

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.38.4"

/**
 * Request body for this edge function
 */
interface ResolveContactsRequest {
  phone_numbers: string[]
}

/**
 * Contact resolution result
 */
interface ContactResolution {
  phone_number: string
  user_id?: string | null
  display_name?: string | null
  has_app: boolean
}

/**
 * Response structure
 */
interface SuccessResponse {
  success: true
  contacts: ContactResolution[]
}

interface ErrorResponse {
  success: false
  error: string
}

/**
 * User from users table
 */
interface User {
  id: string
  phone_number: string
  display_name: string | null
}

/**
 * Supabase service role client for admin operations
 */
const supabaseUrl = Deno.env.get("SUPABASE_URL")!
const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!

const supabase = createClient(supabaseUrl, supabaseServiceKey)

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
    const body: ResolveContactsRequest = await req.json()

    // Validate required fields
    if (!body.phone_numbers || !Array.isArray(body.phone_numbers)) {
      return new Response(
        JSON.stringify({
          success: false,
          error: "Missing or invalid field: phone_numbers (must be array)",
        } as ErrorResponse),
        {
          status: 400,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    // Filter out empty phone numbers
    const phoneNumbers = body.phone_numbers.filter(
      (pn) => pn && typeof pn === "string" && pn.trim().length > 0
    )

    if (phoneNumbers.length === 0) {
      return new Response(
        JSON.stringify({
          success: true,
          contacts: [],
        } as SuccessResponse),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    // Query users table for matching phone numbers
    const { data: users, error: usersError } = await supabase
      .from("users")
      .select("id, phone_number, display_name")
      .in("phone_number", phoneNumbers)

    if (usersError) {
      console.error("Error fetching users:", usersError)
      return new Response(
        JSON.stringify({
          success: false,
          error: "Failed to resolve contacts",
        } as ErrorResponse),
        {
          status: 500,
          headers: { "Content-Type": "application/json" },
        }
      )
    }

    // Build phone number to user ID map
    const userMap = new Map<string, User>()
    if (users) {
      for (const user of users) {
        userMap.set(user.phone_number, user)
      }
    }

    // Build response for each input phone number
    const contacts: ContactResolution[] = phoneNumbers.map(
      (phoneNumber) => {
        const user = userMap.get(phoneNumber)
        return {
          phone_number: phoneNumber,
          user_id: user?.id ?? null,
          display_name: user?.display_name ?? null,
          has_app: !!user,
        }
      }
    )

    return new Response(
      JSON.stringify({
        success: true,
        contacts,
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
