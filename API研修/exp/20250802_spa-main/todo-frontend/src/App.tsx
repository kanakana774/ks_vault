import { useEffect, useState } from 'react'
import axios from 'axios'

type Todo = {
  id: number
  title: string
  completed: boolean
}

function App() {
  const [todos, setTodos] = useState<Todo[]>([])

  useEffect(() => {
    axios.get<Todo[]>('http://localhost:8080/todos')
      .then(response => {
        setTodos(response.data)
      })
      .catch(error => {
        console.error("API呼び出し失敗:", error)
      })
  }, [])

  return (
    <div style={{ padding: '2rem' }}>
      <h1>Todo 一覧</h1>
      <ul>
        {todos.map(todo => (
          <li key={todo.id}>
            {todo.title} {todo.completed ? "✅" : "⬜️"}
          </li>
        ))}
      </ul>
    </div>
  )
}

export default App
