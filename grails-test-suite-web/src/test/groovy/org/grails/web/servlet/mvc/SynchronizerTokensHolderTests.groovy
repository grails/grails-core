package org.grails.web.servlet.mvc

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

class SynchronizerTokensHolderTests {

    @Test
    // GRAILS-9923
    void testSerializable() {
        SynchronizerTokensHolder holder = new SynchronizerTokensHolder()
        holder.generateToken 'url1'
        holder.generateToken 'url1'
        holder.generateToken 'url2'

        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ObjectOutputStream oos = new ObjectOutputStream(baos)
        oos.writeObject holder
        byte[] data = baos.toByteArray()

        ObjectInputStream ios = new ObjectInputStream(new ByteArrayInputStream(data))
        def deserialized = ios.readObject()
        assertTrue deserialized instanceof SynchronizerTokensHolder

        SynchronizerTokensHolder holder2 = deserialized
        assertEquals holder2.currentTokens, holder.currentTokens
        assertEquals 2, holder2.currentTokens.size()

        holder.generateToken 'url3'
        assertEquals 2, holder2.currentTokens.size()

        holder2.generateToken 'url3'
        assertEquals 3, holder2.currentTokens.size()
    }

    @Test
    void testGenerate() {
        SynchronizerTokensHolder holder = new SynchronizerTokensHolder()
        assert holder.empty

        String url1 = 'url1'
        assertNotNull holder.generateToken(url1)
        assertEquals 1, holder.currentTokens.size()
        assertEquals 1, holder.currentTokens[url1].size()

        assertNotNull holder.generateToken(url1)
        assertEquals 1, holder.currentTokens.size()
        assertEquals 2, holder.currentTokens[url1].size()

        String url2 = 'url2'
        assertNotNull holder.generateToken(url2)
        assertEquals 2, holder.currentTokens.size()
        assertEquals 2, holder.currentTokens[url1].size()
        assertEquals 1, holder.currentTokens[url2].size()
    }

    @Test
    void testIsValid() {
        SynchronizerTokensHolder holder = new SynchronizerTokensHolder()
        assertTrue holder.empty

        String url = 'url1'

        String token = holder.generateToken(url)
        assertTrue holder.isValid(url, token)
        assertFalse holder.isValid(url, token + '!')
    }

    @Test
    void testResetTokens() {
        SynchronizerTokensHolder holder = new SynchronizerTokensHolder()
        assertTrue holder.empty

        String url1 = 'url1'
        String url2 = 'url2'

        assertNotNull holder.generateToken(url1)
        assertNotNull holder.generateToken(url2)
        assertEquals 2, holder.currentTokens.size()

        holder.resetToken url1
        assertEquals 1, holder.currentTokens.size()

        holder.resetToken url2
        assertEquals 0, holder.currentTokens.size()
    }

    @Test
    void testResetToken() {
        SynchronizerTokensHolder holder = new SynchronizerTokensHolder()

        String url1 = 'url1'
        String url2 = 'url2'

        String token1 = holder.generateToken(url1)
        String token2 = holder.generateToken(url1)
        String token3 = holder.generateToken(url1)
        String token4 = holder.generateToken(url2)
        assertEquals 2, holder.currentTokens.size()

        holder.resetToken url1, token1
        assertEquals 2, holder.currentTokens.size()

        holder.resetToken url1, token2
        assertEquals 2, holder.currentTokens.size()

        holder.resetToken url1, token3
        assertEquals 1, holder.currentTokens.size()

        holder.resetToken url1, token4
        assertEquals 1, holder.currentTokens.size()

        holder.resetToken url1, token4 + '!'
        assertEquals 1, holder.currentTokens.size()

        holder.resetToken url2, token4
        assertEquals 0, holder.currentTokens.size()
    }
}
