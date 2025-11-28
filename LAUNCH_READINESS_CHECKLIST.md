# Zii Chat v1.0 - Launch Readiness Checklist

## Pre-Launch Requirements

### 1. Core App Features ✅
- [x] Bluetooth mesh networking
- [x] Geohash channels (Nostr)
- [x] End-to-end encryption
- [x] Location notes (with local cache for last 50)
- [x] Tor integration
- [x] Welcome screen with nickname
- [x] About/Settings UI

### 2. Activation System 🔄 IN PROGRESS
- [ ] Activation code validator (Android)
- [ ] Vercel API endpoint
- [ ] Code generator tool
- [ ] Expiry notifications
- [ ] Grace period handling (1h validation, 12h expiry)
- [ ] Activation UI screens

### 3. Payment Integration 📋 PLANNED
- [ ] FastPay integration (R15+ website payments)
- [ ] Website purchase flow
- [ ] WhatsApp support workflow (EFT/cash)
- [ ] Partner onboarding process
- [ ] Code batch management system

### 4. Infrastructure 📋 PLANNED
- [ ] Register domain (api.ziichat.com)
- [ ] Deploy Vercel API
- [ ] Set up Vercel KV database
- [ ] Configure FastPay account
- [ ] Set up monitoring/logging

### 5. Legal & Compliance 📋 NEEDED
- [ ] Terms of Service
- [ ] Privacy Policy (POPIA compliance)
- [ ] Content moderation policy
- [ ] Age restriction policy
- [ ] Data retention policy

### 6. Marketing Materials 📋 NEEDED
- [ ] App description (Google Play)
- [ ] Screenshots (5-8 images)
- [ ] Feature graphic
- [ ] App icon (512x512)
- [ ] Promo video (optional)
- [ ] Website landing page


### 7. Distribution 📋 NEEDED
- [ ] Google Play Developer account
- [ ] App signing key generated
- [ ] APK/AAB built and tested
- [ ] Google Play listing created
- [ ] Direct APK download link (website)

### 8. Support Infrastructure 📋 NEEDED
- [ ] WhatsApp support number
- [ ] Support email (support@ziichat.com)
- [ ] FAQ document
- [ ] Partner onboarding guide
- [ ] User manual/help docs

### 9. Testing 🔄 ONGOING
- [ ] Location notes persistence (fix in progress)
- [ ] Activation code flow (end-to-end)
- [ ] Payment processing (FastPay sandbox)
- [ ] Multi-device testing
- [ ] Battery usage optimization
- [ ] Network connectivity edge cases

### 10. Partner Program 📋 PLANNED
- [ ] Partner pricing structure (20% discount)
- [ ] Partner application form
- [ ] Bulk code generation tool
- [ ] Physical voucher card design
- [ ] Marketing materials for partners

## Launch Strategy

### Soft Launch (Phase 1)
**Timeline**: Week 1-2
- Share APK link with early adopters
- Monitor for critical bugs
- Gather initial feedback
- No paid marketing yet

### Growth Phase (Phase 2)
**Timeline**: Week 3+
- Enable activation system
- Start accepting payments
- Onboard first partners
- Organic growth via word-of-mouth

### Scale Phase (Phase 3)
**Trigger**: 1000+ active users
- Develop Feed feature
- Develop Chatrooms feature
- Consider paid marketing
- Expand partner network

## Critical Path to Launch

### Week 1: Activation System
1. Build activation code validator
2. Create Vercel API
3. Build code generator
4. Test end-to-end flow

### Week 2: Payment & Infrastructure
1. Integrate FastPay
2. Deploy Vercel API
3. Create website purchase flow
4. Set up WhatsApp support

### Week 3: Legal & Marketing
1. Write Terms of Service
2. Write Privacy Policy
3. Create app screenshots
4. Write app description

### Week 4: Testing & Launch
1. Final testing
2. Google Play submission
3. Soft launch (share link)
4. Monitor and iterate

## Success Metrics

### Week 1 Targets
- 10-50 users
- 0 critical bugs
- Basic feedback collected

### Month 1 Targets
- 100-500 users
- 50+ paid activations
- 2-5 partner shops onboarded

### Month 3 Targets
- 1000+ users
- 500+ paid activations
- 10+ partner shops
- Ready for Feed/Chatrooms development

## Risk Mitigation

### Technical Risks
- **Location notes not persisting**: Added local cache (last 50)
- **Relay downtime**: Multiple relay fallbacks
- **Bluetooth issues**: Optimized scanning, clear error messages

### Business Risks
- **Slow adoption**: Soft launch allows time to iterate
- **Payment fraud**: Single-use codes, timestamp validation
- **Partner issues**: Upfront payment protects revenue

### Legal Risks
- **Content moderation**: Clear ToS, report mechanism
- **Privacy compliance**: POPIA-compliant privacy policy
- **Age restriction**: 18+ requirement in ToS

## Post-Launch Priorities

### Immediate (Week 1-2)
1. Fix critical bugs
2. Monitor activation system
3. Respond to user feedback
4. Optimize battery usage

### Short-term (Month 1)
1. Onboard more partners
2. Improve onboarding UX
3. Add in-app help/FAQ
4. Optimize relay selection

### Medium-term (Month 2-3)
1. iOS version planning
2. Feed feature development (if 1000+ users)
3. Chatrooms feature development (if 1000+ users)
4. Marketing campaign

---

**Last Updated**: 2024-11-28  
**Status**: Pre-Launch Planning
