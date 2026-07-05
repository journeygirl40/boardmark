package com.picsearch.app

import com.picsearch.app.util.RedirectUrlUnwrapper
import org.junit.Assert.assertEquals
import org.junit.Test

class RedirectUrlUnwrapperTest {

    @Test
    fun `extracts real url from bing churl parameter`() {
        val bingUrl = "https://www.bing.com/videos/riverview/relatedvideo?q=google&&mid=4D42" +
            "&churl=https%3a%2f%2fwww.youtube.com%2fchannel%2fUCGCZAYq5Xxojl_tSXcVJhiQ&FORM=VRDGAR"

        val result = RedirectUrlUnwrapper.unwrap(bingUrl)

        assertEquals("https://www.youtube.com/channel/UCGCZAYq5Xxojl_tSXcVJhiQ", result)
    }

    @Test
    fun `returns original url when no wrapper pattern matches`() {
        val plainUrl = "https://example.com/article?id=123"

        assertEquals(plainUrl, RedirectUrlUnwrapper.unwrap(plainUrl))
    }

    @Test
    fun `returns original url when bing host has no churl param`() {
        val bingUrlWithoutChurl = "https://www.bing.com/search?q=google"

        assertEquals(bingUrlWithoutChurl, RedirectUrlUnwrapper.unwrap(bingUrlWithoutChurl))
    }
}
