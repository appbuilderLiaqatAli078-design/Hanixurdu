package com.example.domain.model

import com.example.data.model.CategoryItem
import com.example.data.model.Episode
import com.example.data.model.MembershipTier
import com.example.data.model.Movie
import com.example.data.model.Season
import com.example.data.model.Series
import com.example.data.model.User

/**
 * Domain layer model aliases for Clean Architecture alignment (Part 4).
 * Enforces strictly two membership tiers: FREE and VIP.
 */
typealias DomainMembershipTier = MembershipTier
typealias DomainUser = User
typealias DomainMovie = Movie
typealias DomainSeries = Series
typealias DomainSeason = Season
typealias DomainEpisode = Episode
typealias DomainCategory = CategoryItem
