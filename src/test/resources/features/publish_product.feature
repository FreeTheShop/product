Feature: Publishing a product to the shop
  Catalog managers prepare products as drafts and publish them when they are ready to sell.
  Shoppers only ever see published products.

  Scenario: A draft with a variant is published and appears in the shop
    Given a catalog manager has created the draft "RUN-2026" named "Trail Runner" with variant "RUN-2026-42"
    And shoppers cannot see "RUN-2026"
    When the catalog manager publishes "RUN-2026"
    Then shoppers can see "RUN-2026" with variant "RUN-2026-42"

  Scenario: A draft without variants cannot be published
    Given a catalog manager has created the draft "BARE-1" named "Bare" with no variants
    When the catalog manager publishes "BARE-1"
    Then publishing is refused with "PRODUCT_HAS_NO_VARIANTS"
    And shoppers cannot see "BARE-1"

  Scenario: An archived product leaves the shop
    Given a catalog manager has created the draft "OLD-1" named "Old" with variant "OLD-1-S"
    And the catalog manager publishes "OLD-1"
    When the catalog manager archives "OLD-1"
    Then shoppers cannot see "OLD-1"
