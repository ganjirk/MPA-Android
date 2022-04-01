from django.contrib import admin

from .models import Offer, Product_List
# Register your models here.


class Product_ListAdmin(admin.ModelAdmin):
    list_display = ('name','price','stock')

class OfferAdmin(admin.ModelAdmin):
    list_display = ('code','discount')


admin.site.register(Product_List, Product_ListAdmin)
admin.site.register(Offer, OfferAdmin)