package org.grails.web.servlet.mvc


class SynchronizerTokensHolderTests extends GroovyTestCase {

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
        assert deserialized instanceof SynchronizerTokensHolder

        SynchronizerTokensHolder holder2 = deserialized
        assert holder2.currentTokens == holder.currentTokens
        assert 2 == holder2.currentTokens.size()

        holder.generateToken 'url3'
        assert 2 == holder2.currentTokens.size()

        holder2.generateToken 'url3'
        assert 3 == holder2.currentTokens.size()
    }

    void testGenerate() {
        SynchronizerTokensHolder holder = new SynchronizerTokensHolder()
        assert holder.empty

        String url1 = 'url1'
        assert holder.generateToken(url1)
        assert 1 == holder.currentTokens.size()
        assert 1 == holder.currentTokens[url1].size()

        assert holder.generateToken(url1)
        assert 1 == holder.currentTokens.size()
        assert 2 == holder.currentTokens[url1].size()

        String url2 = 'url2'
        assert holder.generateToken(url2)
        assert 2 == holder.currentTokens.size()
        assert 2 == holder.currentTokens[url1].size()
        assert 1 == holder.currentTokens[url2].size()
    }

    void testIsValid() {
        SynchronizerTokensHolder holder = new SynchronizerTokensHolder()
        assert holder.empty

        String url = 'url1'

        String token = holder.generateToken(url)
        assert holder.isValid(url, token)
        assert !holder.isValid(url, token + '!')
    }

    void testResetTokens() {
        SynchronizerTokensHolder holder = new SynchronizerTokensHolder()
        assert holder.empty

        String url1 = 'url1'
        String url2 = 'url2'

        assert holder.generateToken(url1)
        assert holder.generateToken(url2)
        assert 2 == holder.currentTokens.size()

        holder.resetToken url1
        assert 1 == holder.currentTokens.size()

        holder.resetToken url2
        assert 0 == holder.currentTokens.size()
    }

    void testResetToken() {
        SynchronizerTokensHolder holder = new SynchronizerTokensHolder()

        String url1 = 'url1'
        String url2 = 'url2'

        String token1 = holder.generateToken(url1)
        String token2 = holder.generateToken(url1)
        String token3 = holder.generateToken(url1)
        String token4 = holder.generateToken(url2)
        assert 2 == holder.currentTokens.size()

        holder.resetToken url1, token1
        assert 2 == holder.currentTokens.size()

        holder.resetToken url1, token2
        assert 2 == holder.currentTokens.size()

        holder.resetToken url1, token3
        assert 1 == holder.currentTokens.size()

        holder.resetToken url1, token4
        assert 1 == holder.currentTokens.size()

        holder.resetToken url1, token4 + '!'
        assert 1 == holder.currentTokens.size()

        holder.resetToken url2, token4
        assert 0 == holder.currentTokens.size()
    }
}
