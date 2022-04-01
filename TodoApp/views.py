from django.shortcuts import render, redirect

from .models import TodoApp

from .Forms import TodoAppForm

from django.views.decorators.http import require_POST

# Create your views here.

def index(request):
    todo_items = TodoApp.objects.order_by('id')
    form = TodoAppForm()
    context =  {'todo_items' : todo_items, 'form' : form}
    return render(request, 'demoapp1/index.html', context)

@require_POST
def addTodoItem(request):
    form = TodoAppForm(request.POST)
    
    if form.is_valid():
        new_todo = TodoApp(text=request.POST['text'])
        new_todo.save()
    return redirect('index')

def completedTodo (request, todo_id):
    todo = TodoApp.objects.get(pk=todo_id)
    todo.completed = True
    todo.save()

    return redirect('index')

def deleteCompleted (request):
    TodoApp.objects.filter(completed__exact=True).delete()
    
    return redirect('index')

def deleteAll (request):
    TodoApp.objects.all().delete()
    
    return redirect('index')