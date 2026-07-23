# React + TypeScript における useState  
  
## 最重要（これが分かれば使えはする）  
  
### 1. useStateとは何か  
  
- コンポーネント内で状態（State）を保持する仕組み  
- 状態が変わると再レンダリングされる  
  
```tsx  
const [count, setCount] = useState(0);  
```  
  
---  
  
### 2. 戻り値は配列  
  
- 1番目: 現在の状態  
- 2番目: 状態更新関数  
  
```tsx  
const [count, setCount] = useState(0);  
```  
  
| 変数 | 意味 |  
|--------|--------|  
| count | 現在値 |  
| setCount | 更新関数 |  
  
---  
  
### 3. 状態更新は set 関数で行う  
  
NG  
  
```tsx  
count = 1;  
```  
  
OK  
  
```tsx  
setCount(1);  
```  
  
---  
  
### 4. setすると再レンダリングされる  
  
```tsx  
setCount(1);  
```  
  
↓  
  
```tsx  
function Component() {  
  // 再実行される  
}  
```  
  
Reactは再度コンポーネント関数を呼び出す。  
  
---  
  
### 5. useStateはフック（Hook）  
  
- 関数コンポーネントで使う  
- トップレベルで呼ぶ  
  
NG  
  
```tsx  
if (flag) {  
  useState(0);  
}  
```  
  
OK  
  
```tsx  
const [count] = useState(0);  
```  
  
---  
  
## 重要（実務で必須）  
  
### 6. 更新は即時反映されない  
  
```tsx  
setCount(1);  
  
console.log(count);  
```  
  
出力  
  
```text  
0  
```  
  
になることがある。  
  
Reactが次のレンダリングで反映するため。  
  
---  
  
### 7. 前回値を使う場合は関数形式を使う  
  
NG  
  
```tsx  
setCount(count + 1);  
setCount(count + 1);  
```  
  
期待  
  
```text  
+2  
```  
  
実際  
  
```text  
+1  
```  
  
になることがある。  
  
OK  
  
```tsx  
setCount(prev => prev + 1);  
setCount(prev => prev + 1);  
```  
  
---  
  
### 8. Stateは不変（Immutable）として扱う  
  
NG  
  
```tsx  
user.name = "Tanaka";  
```  
  
OK  
  
```tsx  
setUser({  
  ...user,  
  name: "Tanaka",  
});  
```  
  
---  
  
### 9. useStateの型指定  
  
基本  
  
```tsx  
const [count, setCount] = useState<number>(0);  
```  
  
推論できる場合  
  
```tsx  
const [count, setCount] = useState(0);  
```  
  
---  
  
### 10. nullを扱う場合  
  
```tsx  
const [user, setUser] = useState<User | null>(null);  
```  
  
TypeScriptでは非常に頻出。  
  
---  
  
## 理解が深まる（なぜそうなるのか）  
  
### 11. Stateはレンダリングごとに固定される  
  
```tsx  
function Counter() {  
  const [count, setCount] = useState(0);  
  
  const handleClick = () => {  
    console.log(count);  
  };  
}  
```  
  
`handleClick` はそのレンダリング時点の `count` を覚えている。  
  
クロージャの理解につながる。  
  
---  
  
### 12. Stateはコンポーネントごとに独立している  
  
```tsx  
<Counter />  
<Counter />  
```  
  
それぞれ別のStateを持つ。  
  
---  
  
### 13. State変更時にReactが差分更新する  
  
```tsx  
setCount(1);  
```  
  
↓  
  
仮想DOM比較  
  
↓  
  
必要箇所のみ更新  
  
---  
  
## 発展（余裕があれば）  
  
### 14. 初期値は初回レンダリング時のみ利用される  
  
```tsx  
const [count] = useState(props.initialCount);  
```  
  
propsが変わっても初期値は再適用されない。  
  
---  
  
### 15. 遅延初期化  
  
```tsx  
const [data] = useState(() => {  
  return heavyCalculation();  
});  
```  
  
重い処理を初回レンダリング時のみ実行できる。  
  
---  
  
### 16. Stateが複雑ならuseReducerも検討する  
  
```tsx  
const [state, dispatch] = useReducer(...);  
```  
  
複雑な更新ロジック向け。  
  

---

クロージャ


```js
const count = 100;  
  
function outer() {  
  const count = 10;  
  
  return function inner() {  
    console.log(count);  
  };  
}  
  
const fn = outer();  
  
function another() {  
  const count = 999;  
  
  fn();  
}  
  
another();
```